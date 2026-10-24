package com.renzoproject.calc_api.electrical.generatorset;

import jakarta.validation.constraints.Positive;

/**
 * A pump motor, mirroring calc-core's {@code GeneratorSetSizingInput.PumpMotor}. Give either the
 * horsepower with its voltage (read from the PEC tables) or the full-load current; not both.
 *
 * @param horsepowerLabel horsepower as printed in the PEC tables, e.g. {@code "100"}
 * @param voltage         motor voltage, with {@code horsepowerLabel}
 * @param fullLoadAmps    full-load current in amperes at the system voltage
 * @param lockedRotorAmps locked-rotor current in amperes at the system voltage; for the fire pump motor
 *                        entered by its currents, optional (without it its starting is left out)
 */
public record PumpMotorDto(
		String horsepowerLabel,
		@Positive Integer voltage,
		@Positive Double fullLoadAmps,
		@Positive Double lockedRotorAmps) {

}
