package com.renzoproject.calc.core.electrical.reference;

/**
 * One row of PEC Table 3.10.2.6(B)(3)(a) — ampacity adjustment factors for more than three
 * current-carrying conductors in a raceway/cable.
 *
 * @param conductorCountMax nullable — {@code null} for the open-ended "41 and above" row
 * @param adjustmentFactorPercent published percentage, not yet divided by 100 (e.g. {@code 80},
 *                                not {@code 0.8}) — see {@link ConductorCountAdjustmentTable#lookup}
 *                                for the divided form
 */
public record ConductorCountAdjustmentEntry(
		String conductorCountRangeLabel,
		int conductorCountMin,
		Integer conductorCountMax,
		double adjustmentFactorPercent) {

}
