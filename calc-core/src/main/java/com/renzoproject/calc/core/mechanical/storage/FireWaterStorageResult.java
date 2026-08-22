package com.renzoproject.calc.core.mechanical.storage;

import javax.measure.Quantity;
import javax.measure.quantity.Volume;

/**
 * Result of {@link FireWaterStorageCalculator}.
 *
 * @param resolvedDurationMinutesMin lower bound of the resolved {@link DurationRange}
 * @param resolvedDurationMinutesMax upper bound of the resolved {@link DurationRange}
 * @param durationMinutesUsed        whichever value actually went into the volume calculation
 * @param usedConservativeDefault    {@code true} if {@code selectedDurationMinutes} was
 *                                    {@code null} and {@code resolvedDurationMinutesMax} was
 *                                    used instead
 * @param requiredStorageVolume      reported as an Indriya {@code Quantity<Volume>} in US
 *                                    gallons at this level ({@code ratedPumpFlow(GPM) * durationMinutesUsed}
 *                                    is dimensionally gallons directly) -- convert via
 *                                    {@link Quantity#to} for any other unit; the display layer
 *                                    owns that choice, not this calculator
 */
public record FireWaterStorageResult(
		double resolvedDurationMinutesMin,
		double resolvedDurationMinutesMax,
		double durationMinutesUsed,
		boolean usedConservativeDefault,
		Quantity<Volume> requiredStorageVolume) {

}
