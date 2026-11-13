package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.HazenWilliams;
import com.renzoproject.calc.core.mechanical.firepump.FirePumpUnits;
import com.renzoproject.calc.core.mechanical.pipe.PipeDimensionResolver;
import com.renzoproject.calc.core.mechanical.pipe.PipeSizingMode;
import com.renzoproject.calc.core.mechanical.pipe.PipeSizingResult;
import com.renzoproject.calc.core.mechanical.pipe.PipeVelocityCalculator;
import com.renzoproject.calc.core.mechanical.pipe.PipeVelocityInput;
import com.renzoproject.calc.core.mechanical.pipe.RawDiameter;
import com.renzoproject.calc.core.mechanical.pipe.VelocityResult;
import tech.units.indriya.quantity.Quantities;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * One pipe segment's line of the hydraulic calculation: its actual bore, its fittings' equivalent
 * length (rescaled from the chart's C factor to the segment's own), the Hazen-Williams friction, the
 * elevation change and the velocity -- and the same line rescaled to a higher flow after junction
 * balancing. Part of {@link SprinklerHydraulicCalculator}; the gravity tank's outlet runs use its
 * fitting lookup too.
 */
final class SegmentRowBuilder {

	private static final String VERIFIED_CONFIDENCE = "verified";

	private final PipeDimensionResolver dimensionResolver;
	private final FittingEquivalentLengthResolver fittingResolver;
	private final PipeVelocityCalculator velocityCalculator;

	SegmentRowBuilder(
			PipeDimensionResolver dimensionResolver,
			FittingEquivalentLengthResolver fittingResolver,
			PipeVelocityCalculator velocityCalculator) {
		this.dimensionResolver = dimensionResolver;
		this.fittingResolver = fittingResolver;
		this.velocityCalculator = velocityCalculator;
	}

	SegmentFlowRow computeRow(
			SprinklerPipeSegment segment, double flowGpm, double upstreamPressurePsi, Set<String> warnings) {
		double insideDiameterInch = dimensionResolver
				.resolve(segment.pipeMaterial(), segment.schedule(), segment.nominalSize())
				.internalDiameter()
				.to(SprinklerUnits.INCH)
				.getValue()
				.doubleValue();
		FittingLengths fittingLengths = fittingEquivalentLengthFt(segment, warnings);
		return buildRow(segment, flowGpm, upstreamPressurePsi, insideDiameterInch, fittingLengths);
	}

	private SegmentFlowRow buildRow(
			SprinklerPipeSegment segment,
			double flowGpm,
			double upstreamPressurePsi,
			double insideDiameterInch,
			FittingLengths fittingLengths) {
		double fittingLengthFt = fittingLengths.totalFt();
		double totalLengthFt = segment.pipeLengthFt() + fittingLengthFt;
		double psiPerFoot = HazenWilliams.frictionLossPsiPerFoot(flowGpm, segment.cFactor(), insideDiameterInch);
		double frictionPsi = psiPerFoot * totalLengthFt;
		double elevationPsi = ElevationPressure.psiForRise(segment.elevationChangeFt());
		double endPressurePsi = upstreamPressurePsi + frictionPsi + elevationPsi;

		return new SegmentFlowRow(
				segment.fromNodeId(),
				segment.toNodeId(),
				flowGpm,
				segment.nominalSize(),
				insideDiameterInch,
				segment.cFactor(),
				segment.pipeLengthFt(),
				fittingLengthFt,
				fittingLengths.cFactorAdjustment(),
				totalLengthFt,
				psiPerFoot,
				frictionPsi,
				elevationPsi,
				endPressurePsi,
				velocityFps(flowGpm, insideDiameterInch));
	}

	/**
	 * Rescales a branch's rows to a higher flow after junction balancing. Each row keeps its
	 * original upstream pressure and has its own friction loss recomputed at the scaled flow --
	 * see this class's Javadoc for why that is the standard approximation rather than a full
	 * re-solve.
	 */
	List<SegmentFlowRow> rescale(List<SegmentFlowRow> rows, double factor) {
		List<SegmentFlowRow> rescaled = new ArrayList<>(rows.size());
		for (SegmentFlowRow row : rows) {
			double upstreamPressurePsi = row.endPressurePsi() - row.frictionLossPsi() - row.elevationPressurePsi();
			double scaledFlowGpm = row.flowGpm() * factor;
			double psiPerFoot = HazenWilliams.frictionLossPsiPerFoot(scaledFlowGpm, row.cFactor(), row.insideDiameterInch());
			double frictionPsi = psiPerFoot * row.totalLengthFt();

			rescaled.add(new SegmentFlowRow(
					row.fromNodeId(),
					row.toNodeId(),
					scaledFlowGpm,
					row.nominalSize(),
					row.insideDiameterInch(),
					row.cFactor(),
					row.pipeLengthFt(),
					row.fittingEquivalentLengthFt(),
					row.fittingCFactorAdjustment(),
					row.totalLengthFt(),
					psiPerFoot,
					frictionPsi,
					row.elevationPressurePsi(),
					upstreamPressurePsi + frictionPsi + row.elevationPressurePsi(),
					velocityFps(scaledFlowGpm, row.insideDiameterInch())));
		}
		return rescaled;
	}

	/**
	 * Sums a segment's fittings. Lengths looked up from the published chart are rescaled from its
	 * C=120 basis to the segment's own C, as NFPA 13 requires; an explicitly supplied length is
	 * used exactly as given, because the caller's basis is unknown and silently rescaling someone
	 * else's number would be worse than leaving it alone.
	 */
	private FittingLengths fittingEquivalentLengthFt(SprinklerPipeSegment segment, Set<String> warnings) {
		return fittingEquivalentLengthFt(segment.nominalSize(), segment.cFactor(), segment.fittings(), warnings);
	}

	/**
	 * Takes the three things that actually matter rather than a segment, so a gravity tank's outlet
	 * runs resolve their fittings through exactly this code -- including the C factor rescaling and
	 * the unverified-data warning -- instead of a parallel copy that could drift from it.
	 */
	FittingLengths fittingEquivalentLengthFt(
			String nominalSize, double cFactor, List<SegmentFitting> fittings, Set<String> warnings) {
		double publishedFtTotal = 0;
		double explicitFtTotal = 0;

		for (SegmentFitting fitting : fittings) {
			if (fitting.explicitEquivalentLengthFt() != null) {
				explicitFtTotal += fitting.quantity() * fitting.explicitEquivalentLengthFt();
				continue;
			}

			FittingEquivalentLengthRow row = fittingResolver.resolve(fitting.fittingType());
			Double publishedFt = row.equivalentLengthFtByNominalSize().get(nominalSize);
			if (publishedFt == null) {
				throw new CalculationException("No published equivalent length for fitting " + fitting.fittingType()
						+ " at nominal size " + nominalSize + " (reference data confidence: "
						+ row.confidence() + ") -- supply NFPA 13 Table 27.2.3.1.1 in "
						+ "reference/sprinkler/fitting-equivalent-lengths.json, or pass an "
						+ "explicitEquivalentLengthFt on the fitting");
			}
			if (!VERIFIED_CONFIDENCE.equalsIgnoreCase(row.confidence())) {
				warnings.add("Fitting equivalent length for " + row.fittingType() + " is reference data tagged '"
						+ row.confidence() + "' -- confirm against NFPA 13 Table 27.2.3.1.1 before relying on this "
						+ "calculation");
			}
			publishedFtTotal += fitting.quantity() * publishedFt;
		}

		if (publishedFtTotal == 0) {
			return new FittingLengths(explicitFtTotal, 1.0);
		}

		double adjustment = HazenWilliams.equivalentLengthAdjustmentFactor(
				cFactor, fittingResolver.cFactorBasis());
		return new FittingLengths(publishedFtTotal * adjustment + explicitFtTotal, adjustment);
	}

	/** Reuses {@link PipeVelocityCalculator} rather than repeating {@code V = Q/A}. */
	private double velocityFps(double flowGpm, double insideDiameterInch) {
		if (flowGpm <= 0) {
			return 0;
		}
		PipeVelocityInput velocityInput = new PipeVelocityInput(
				PipeSizingMode.VELOCITY_FROM_DIAMETER,
				Quantities.getQuantity(flowGpm, FirePumpUnits.GPM),
				new RawDiameter(Quantities.getQuantity(insideDiameterInch, SprinklerUnits.INCH)),
				null,
				null,
				null);
		PipeSizingResult result = velocityCalculator.calculate(velocityInput);
		if (result instanceof VelocityResult velocity) {
			return velocity.velocity().to(SprinklerUnits.FOOT_PER_SECOND).getValue().doubleValue();
		}
		throw new CalculationException("PipeVelocityCalculator returned " + result.getClass().getSimpleName()
				+ " for a VELOCITY_FROM_DIAMETER request, which should never happen");
	}

	/**
	 * A segment's total fitting equivalent length and the C factor adjustment that produced it
	 * ({@code 1.0} when nothing was looked up from the published chart).
	 */
	record FittingLengths(double totalFt, double cFactorAdjustment) {
	}

}
