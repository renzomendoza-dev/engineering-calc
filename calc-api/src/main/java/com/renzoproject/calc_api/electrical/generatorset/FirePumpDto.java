package com.renzoproject.calc_api.electrical.generatorset;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;

/**
 * A fire pump the generator carries as the alternate source (PEC Article 6.95), mirroring
 * calc-core's {@code GeneratorSetSizingInput.FirePump}.
 *
 * @param motor                  the fire pump motor
 * @param jockey                 the pressure maintenance pump; blank for none. Only its full-load current is used.
 * @param accessoryAmps          full-load current of the accessory equipment at the system voltage; blank for none
 * @param startStep              start step of the fire pump motor; blank means step 1
 * @param startingCurrentPercent starting current as a percent of its locked-rotor current, above 0 and at most
 *                               100; blank means 100
 * @param supplyRun              the run to the controller; blank skips the conductor sizing
 */
public record FirePumpDto(
		@NotNull @Valid PumpMotorDto motor,
		@Valid PumpMotorDto jockey,
		@PositiveOrZero Double accessoryAmps,
		@Min(1) Integer startStep,
		@Positive @DecimalMax("100") Double startingCurrentPercent,
		@Valid SupplyRunDto supplyRun) {

}
