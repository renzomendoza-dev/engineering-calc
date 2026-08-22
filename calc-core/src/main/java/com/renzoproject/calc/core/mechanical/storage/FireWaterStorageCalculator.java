package com.renzoproject.calc.core.mechanical.storage;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.firepump.FirePumpUnits;
import tech.units.indriya.quantity.Quantities;

/**
 * NFPA 13 water supply duration sizing: required storage volume from a rated fire pump flow and
 * hazard classification. GPM-native throughout, matching {@code mechanical.firepump}'s
 * convention (reuses {@link FirePumpUnits#GPM} rather than converting to SI internally) -- unlike
 * {@link DomesticWaterStorageCalculator}, which is SI.
 *
 * <p>Deliberately standalone: {@code ratedPumpFlow} is a plain input, not a constructor
 * dependency on {@code FirePumpCapacityResolver} or any fire pump suite calculator, so this
 * calculator has no code coupling to that package (only reuses its unit-of-measure constant).
 *
 * <p>Hose stream allowance ({@link DurationRange#hoseStreamAllowanceGpm()}) is deliberately NOT
 * added to {@code ratedPumpFlow} or the resulting volume -- it's exposed on the resolved range
 * for a future enhancement, not consumed here. See {@code reference/storage/README.md}.
 */
public class FireWaterStorageCalculator implements Calculator<FireWaterStorageInput, FireWaterStorageResult> {

	private final FireWaterDurationResolver durationResolver;

	public FireWaterStorageCalculator(FireWaterDurationResolver durationResolver) {
		this.durationResolver = durationResolver;
	}

	@Override
	public FireWaterStorageResult calculate(FireWaterStorageInput input) {
		DurationRange range = durationResolver.resolve(input.hazardClassification());

		double durationMinutesUsed;
		boolean usedConservativeDefault;
		if (input.selectedDurationMinutes() == null) {
			durationMinutesUsed = range.maxMinutes();
			usedConservativeDefault = true;
		} else {
			double selected = input.selectedDurationMinutes();
			if (selected < range.minMinutes() || selected > range.maxMinutes()) {
				throw new CalculationException("selectedDurationMinutes " + selected + " is outside the valid range for "
						+ input.hazardClassification() + " (" + range.minMinutes() + " to " + range.maxMinutes() + " minutes)");
			}
			durationMinutesUsed = selected;
			usedConservativeDefault = false;
		}

		double ratedPumpFlowGpm = input.ratedPumpFlow().to(FirePumpUnits.GPM).getValue().doubleValue();
		double gallons = ratedPumpFlowGpm * durationMinutesUsed * (1 + input.safetyMarginPercent() / 100.0);

		return new FireWaterStorageResult(
				range.minMinutes(),
				range.maxMinutes(),
				durationMinutesUsed,
				usedConservativeDefault,
				Quantities.getQuantity(gallons, StorageUnits.GALLON_US));
	}

}
