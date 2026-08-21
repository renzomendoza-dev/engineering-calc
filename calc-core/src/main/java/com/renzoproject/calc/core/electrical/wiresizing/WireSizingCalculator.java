package com.renzoproject.calc.core.electrical.wiresizing;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.electrical.reference.AmbientTempCorrectionTable;
import com.renzoproject.calc.core.electrical.reference.AmpacityTable;
import com.renzoproject.calc.core.electrical.reference.ConductorCountAdjustmentTable;
import com.renzoproject.calc.core.electrical.reference.ConductorMaterial;
import com.renzoproject.calc.core.electrical.reference.ConductorProperties;
import com.renzoproject.calc.core.electrical.reference.ConductorPropertiesResolver;
import com.renzoproject.calc.core.electrical.reference.ConductorSize;
import com.renzoproject.calc.core.electrical.reference.InsulationTypeTempRating;
import com.renzoproject.calc.core.electrical.voltagedrop.VoltageDropCalculator;
import com.renzoproject.calc.core.electrical.voltagedrop.VoltageDropInput;
import com.renzoproject.calc.core.electrical.voltagedrop.VoltageDropResult;
import com.renzoproject.calc.core.exception.CalculationException;

import java.util.List;

/**
 * Recommends the smallest conductor size that satisfies PEC ampacity requirements (base
 * ampacity, ambient temperature correction, and conductor-count adjustment), with an optional
 * voltage drop cross-check.
 *
 * <p>Needs reference data beyond pure math — like {@code ConduitFillCalculator}, all reference
 * tables, plus (only exercised when a voltage drop check is requested)
 * {@link ConductorPropertiesResolver} and {@link VoltageDropCalculator}, are plainly
 * instantiated, matching the pattern the other calculators already use (calc-core has no
 * Spring dependency, so there's no DI to reach for here).
 */
public class WireSizingCalculator implements Calculator<WireSizingInput, WireSizingResult> {

	private static final double CONTINUOUS_LOAD_FACTOR = 1.25;

	private final AmpacityTable ampacityTable = new AmpacityTable();
	private final InsulationTypeTempRating insulationTypeTempRating = new InsulationTypeTempRating();
	private final AmbientTempCorrectionTable ambientTempCorrectionTable = new AmbientTempCorrectionTable();
	private final ConductorCountAdjustmentTable conductorCountAdjustmentTable = new ConductorCountAdjustmentTable();
	private final ConductorPropertiesResolver conductorPropertiesResolver = new ConductorPropertiesResolver();
	private final VoltageDropCalculator voltageDropCalculator = new VoltageDropCalculator();

	@Override
	public WireSizingResult calculate(WireSizingInput input) {
		double requiredAmpacityAmps = input.loadCurrentAmps() * (input.isContinuousLoad() ? CONTINUOUS_LOAD_FACTOR : 1.0);
		// Splitting the run into N parallel conductors means each one only has to carry 1/N of
		// the total — this is the value actually sized against below, not requiredAmpacityAmps.
		double requiredAmpacityPerSetAmps = requiredAmpacityAmps / input.numberOfParallelSets();

		int tempRatingCelsius = insulationTypeTempRating.lookup(input.insulationType(), input.conductorMaterial());
		double tempCorrectionFactor = ambientTempCorrectionTable.lookup(input.ambientTempCelsius(), tempRatingCelsius);
		double adjustmentFactor = conductorCountAdjustmentTable.lookup(input.numberOfCurrentCarryingConductors());

		List<ConductorSize> sizesAscending = ampacityTable.allSizesSortedAscendingByArea();

		ConductorSize recommendedSize = null;
		double baseAmpacityAmps = 0;
		double deratedAmpacityAmps = 0;
		for (ConductorSize size : sizesAscending) {
			Double baseAmpacity = lookupAmpacityOrNull(input.conductorMaterial(), size.label(), tempRatingCelsius);
			if (baseAmpacity == null) {
				continue;
			}
			double derated = baseAmpacity * tempCorrectionFactor * adjustmentFactor;
			if (derated >= requiredAmpacityPerSetAmps) {
				recommendedSize = size;
				baseAmpacityAmps = baseAmpacity;
				deratedAmpacityAmps = derated;
				break;
			}
		}

		if (recommendedSize == null) {
			throw new CalculationException("No conductor size in the table satisfies the required ampacity under these conditions"
					+ (input.numberOfParallelSets() > 1 ? " (per conductor, splitting the load across " + input.numberOfParallelSets() + " parallel sets)" : ""));
		}

		boolean meetsTerminationRating = checkMeetsTerminationRating(input, recommendedSize, adjustmentFactor, requiredAmpacityPerSetAmps);

		VoltageDropCheckResult voltageDropCheckResult = input.voltageDropCheck() == null
				? null
				: performVoltageDropCheck(input, recommendedSize, sizesAscending, tempRatingCelsius,
						tempCorrectionFactor, adjustmentFactor, requiredAmpacityPerSetAmps);

		return new WireSizingResult(
				recommendedSize.label(),
				baseAmpacityAmps,
				tempCorrectionFactor,
				adjustmentFactor,
				deratedAmpacityAmps,
				requiredAmpacityAmps,
				input.numberOfParallelSets(),
				requiredAmpacityPerSetAmps,
				meetsTerminationRating,
				voltageDropCheckResult);
	}

	/**
	 * The shared size list spans all materials, so a given size may have no published ampacity
	 * for one material (e.g. aluminum below a minimum branch-circuit size) — skip that size
	 * rather than aborting the walk.
	 */
	private Double lookupAmpacityOrNull(ConductorMaterial material, String sizeLabel, int tempRatingCelsius) {
		try {
			return ampacityTable.lookup(material, sizeLabel, tempRatingCelsius);
		} catch (CalculationException e) {
			return null;
		}
	}

	/**
	 * Recomputes temp correction and re-looks-up ampacity at
	 * {@code terminationTempRatingCelsius} for the same recommended size — deliberately a
	 * fresh lookup, not a reuse of the insulation temp rating's correction factor, since the
	 * two temperature ratings can differ.
	 */
	private boolean checkMeetsTerminationRating(WireSizingInput input, ConductorSize recommendedSize, double adjustmentFactor, double requiredAmpacityPerSetAmps) {
		double terminationCorrectionFactor = ambientTempCorrectionTable.lookup(
				input.ambientTempCelsius(), input.terminationTempRatingCelsius());
		double terminationBaseAmpacity = ampacityTable.lookup(
				input.conductorMaterial(), recommendedSize.label(), input.terminationTempRatingCelsius());
		double terminationDeratedAmpacity = terminationBaseAmpacity * terminationCorrectionFactor * adjustmentFactor;
		return terminationDeratedAmpacity >= requiredAmpacityPerSetAmps;
	}

	/**
	 * Deliberately isolated from the ampacity sizing above — see class Javadoc. If the
	 * ampacity-based recommendation fails voltage drop, walks up to larger sizes looking for
	 * one that satisfies both the (re-verified) ampacity requirement and voltage drop. If the
	 * table is exhausted without finding one, {@code upsizedRecommendation} stays {@code null}
	 * — the caller sees {@code exceedsRecommendedLimit = true} with no fix found, a legitimate
	 * outcome (e.g. "consider parallel sets instead") rather than an error, so this
	 * deliberately does not throw.
	 */
	private VoltageDropCheckResult performVoltageDropCheck(
			WireSizingInput input,
			ConductorSize recommendedSize,
			List<ConductorSize> sizesAscending,
			int tempRatingCelsius,
			double tempCorrectionFactor,
			double adjustmentFactor,
			double requiredAmpacityPerSetAmps) {

		VoltageDropCheckRequest vdCheck = input.voltageDropCheck();
		VoltageDropResult checkedResult = runVoltageDropForSize(input, vdCheck, recommendedSize);

		if (!checkedResult.exceedsRecommendedLimit()) {
			return new VoltageDropCheckResult(recommendedSize.label(), checkedResult.voltageDropPercent(), false, null);
		}

		String upsizedRecommendation = null;
		int startIndex = sizesAscending.indexOf(recommendedSize) + 1;
		for (int i = startIndex; i < sizesAscending.size(); i++) {
			ConductorSize candidate = sizesAscending.get(i);
			Double baseAmpacity = lookupAmpacityOrNull(input.conductorMaterial(), candidate.label(), tempRatingCelsius);
			if (baseAmpacity == null) {
				continue;
			}
			double derated = baseAmpacity * tempCorrectionFactor * adjustmentFactor;
			if (derated < requiredAmpacityPerSetAmps) {
				continue;
			}
			VoltageDropResult candidateResult = runVoltageDropForSize(input, vdCheck, candidate);
			if (!candidateResult.exceedsRecommendedLimit()) {
				upsizedRecommendation = candidate.label();
				break;
			}
		}

		return new VoltageDropCheckResult(recommendedSize.label(), checkedResult.voltageDropPercent(), true, upsizedRecommendation);
	}

	private VoltageDropResult runVoltageDropForSize(WireSizingInput input, VoltageDropCheckRequest vdCheck, ConductorSize size) {
		ConductorProperties properties = conductorPropertiesResolver.resolve(
				vdCheck.circuitType(), size, input.conductorMaterial(), vdCheck.conduitMaterial());
		VoltageDropInput vdInput = new VoltageDropInput(
				vdCheck.circuitType(),
				input.loadCurrentAmps(),
				vdCheck.oneWayLengthMeters(),
				properties.resistanceOhmsPerMeter(),
				properties.reactanceOhmsPerMeter(),
				vdCheck.powerFactor(),
				vdCheck.systemVoltage(),
				vdCheck.parallelSetsPerPhase());
		return voltageDropCalculator.calculate(vdInput);
	}

}
