package com.renzoproject.calc_api.electrical.generatorset;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * A motor, mirroring calc-core's {@code GeneratorSetSizingInput.MotorLoad}.
 *
 * @param label           name shown on the load schedule
 * @param horsepowerLabel horsepower as printed in the PEC full-load current tables, e.g. {@code "7 1/2"}
 * @param voltage         motor voltage, which must have a published full-load current at that horsepower
 * @param quantity        number of identical motors running together
 */
public record MotorLoadDto(
		String label,
		@NotBlank String horsepowerLabel,
		@Positive int voltage,
		@Min(1) int quantity) {

}
