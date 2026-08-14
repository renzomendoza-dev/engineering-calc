package com.renzoproject.calc.core.electrical.conduitfill;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.electrical.reference.ConductorDimensionTable;
import com.renzoproject.calc.core.electrical.reference.ConduitDimensionEntry;
import com.renzoproject.calc.core.electrical.reference.ConduitDimensionTable;
import com.renzoproject.calc.core.electrical.reference.FillPercentageRule;

import java.util.List;

/**
 * Finds the smallest conduit trade size (for a given conduit type) that fits a set of
 * conductors, per PEC Table 10.1.1.1's fill percentage rule.
 *
 * <p>Unlike {@code VoltageDropCalculator}, this calculator needs reference data — it looks up
 * conductor cross-sectional areas via {@link ConductorDimensionTable} and conduit capacities
 * via {@link ConduitDimensionTable}, both plainly instantiated (calc-core has no Spring
 * dependency, so there's no DI to reach for here).
 */
public class ConduitFillCalculator implements Calculator<ConduitFillInput, ConduitFillResult> {

	private final ConductorDimensionTable conductorDimensionTable = new ConductorDimensionTable();
	private final ConduitDimensionTable conduitDimensionTable = new ConduitDimensionTable();

	@Override
	public ConduitFillResult calculate(ConduitFillInput input) {
		double totalConductorAreaMm2 = 0.0;
		int totalConductorCount = 0;

		for (ConductorFillEntry entry : input.conductors()) {
			double areaPerConductor = conductorDimensionTable.lookup(entry.insulationType(), entry.sizeLabel());
			totalConductorAreaMm2 += areaPerConductor * entry.quantity();
			totalConductorCount += entry.quantity();
		}

		double allowedFillPercent = FillPercentageRule.allowedFillPercent(totalConductorCount);

		List<ConduitDimensionEntry> entries = conduitDimensionTable.getEntriesForType(input.conduitType());

		for (ConduitDimensionEntry entry : entries) {
			double maxUsableAreaMm2 = usableAreaColumn(entry, totalConductorCount);
			if (maxUsableAreaMm2 >= totalConductorAreaMm2) {
				double actualFillPercent = (totalConductorAreaMm2 / entry.totalArea100PercentMm2()) * 100.0;
				return new ConduitFillResult(
						String.valueOf(entry.racewaySizeMm()),
						totalConductorAreaMm2,
						totalConductorCount,
						allowedFillPercent,
						actualFillPercent,
						false,
						buildPracticalFillAdvisory(actualFillPercent));
			}
		}

		return new ConduitFillResult(
				null,
				totalConductorAreaMm2,
				totalConductorCount,
				allowedFillPercent,
				null,
				true,
				null);
	}

	/**
	 * Practical, field-experience pull-ease note. Deliberately isolated from the PEC compliance
	 * calculation above — see {@link PracticalFillAdvisory} for why this must never be
	 * conflated with code compliance.
	 */
	private static PracticalFillAdvisory buildPracticalFillAdvisory(double actualFillPercent) {
		boolean difficult = actualFillPercent > PracticalFillAdvisory.PRACTICAL_PULL_THRESHOLD_PERCENT;
		String note = difficult
				? "Fill exceeds " + PracticalFillAdvisory.PRACTICAL_PULL_THRESHOLD_PERCENT + "% - twisted or "
						+ "stranded conductor bundles often pull harder than single conductors at this density, "
						+ "even though this is within PEC's legal fill limit. Consider a larger conduit or fewer "
						+ "conductors per run for easier pulling."
				: null;
		return new PracticalFillAdvisory(difficult, note);
	}

	/**
	 * Uses the table's pre-computed percentage columns directly rather than recomputing from
	 * {@code totalArea100PercentMm2 * allowedFillPercent / 100}, since the source table already
	 * provides exactly these thresholds.
	 */
	private static double usableAreaColumn(ConduitDimensionEntry entry, int conductorCount) {
		if (conductorCount == 1) {
			return entry.oneWire53PercentMm2();
		}
		if (conductorCount == 2) {
			return entry.twoWires53PercentMm2();
		}
		return entry.over2Wires40PercentMm2();
	}

}
