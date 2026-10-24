package com.renzoproject.calc_api.electrical.generatorset;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * A load entered as a total (a building's whole load, or a fire pump) rather than built up,
 * mirroring calc-core's {@code GeneratorSetSizingInput.DirectLoad}. It is taken at 100%, so a
 * building's load is entered after the caller's own demand factors.
 *
 * @param label                  name shown on the load schedule
 * @param kind                   BUILDING or FIRE_PUMP
 * @param amount                 the load, in {@code unit}
 * @param unit                   KVA, KW or AMPS (amperes at the system voltage)
 * @param powerFactor            power factor of the load, above 0 and at most 1; required for KW
 * @param continuous             whether the load runs three hours or more
 * @param lockedRotorAmps        a fire pump's locked-rotor current in amperes at the system voltage, for the
 *                               starting check; blank leaves its starting out. Not allowed for a building.
 * @param startStep              the fire pump's start step; blank means step 1
 * @param startingCurrentPercent starting current as a percent of {@code lockedRotorAmps}, above 0 and at most
 *                               100; blank means 100
 */
public record DirectLoadDto(
		String label,
		@NotBlank String kind,
		@Positive double amount,
		@NotBlank String unit,
		@Positive @DecimalMax("1") Double powerFactor,
		boolean continuous,
		@Positive Double lockedRotorAmps,
		@Min(1) Integer startStep,
		@Positive @DecimalMax("100") Double startingCurrentPercent) {

}
