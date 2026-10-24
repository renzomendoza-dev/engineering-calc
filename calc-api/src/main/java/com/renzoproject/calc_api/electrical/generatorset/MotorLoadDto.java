package com.renzoproject.calc_api.electrical.generatorset;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * A motor, mirroring calc-core's {@code GeneratorSetSizingInput.MotorLoad}.
 *
 * @param label                  name shown on the load schedule
 * @param horsepowerLabel        horsepower as printed in the PEC full-load current tables, e.g. {@code "7 1/2"}
 * @param voltage                motor voltage, which must have a published full-load current at that horsepower
 * @param quantity               number of identical motors running together
 * @param startStep              when it starts: motors in the same step start together, a higher step starts
 *                               once the lower steps are running; blank means step 1
 * @param startingCurrentPercent starting current as a percent of the table's locked-rotor current, above 0 and
 *                               at most 100; blank means 100 (across the line)
 */
public record MotorLoadDto(
		String label,
		@NotBlank String horsepowerLabel,
		@Positive int voltage,
		@Min(1) int quantity,
		@Min(1) Integer startStep,
		@Positive @DecimalMax("100") Double startingCurrentPercent) {

}
