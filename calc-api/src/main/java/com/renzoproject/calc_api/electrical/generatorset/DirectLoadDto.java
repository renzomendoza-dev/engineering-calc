package com.renzoproject.calc_api.electrical.generatorset;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * A building's whole load entered as a total rather than built up, mirroring calc-core's
 * {@code GeneratorSetSizingInput.DirectLoad}. It is taken at 100%, so the load is entered after the
 * caller's own demand factors. (A fire pump goes in {@link FirePumpDto}.)
 *
 * @param label       name shown on the load schedule
 * @param amount      the load, in {@code unit}
 * @param unit        KVA, KW or AMPS (amperes at the system voltage)
 * @param powerFactor power factor of the load, above 0 and at most 1; required for KW
 * @param continuous  whether the load runs three hours or more
 */
public record DirectLoadDto(
		String label,
		@Positive double amount,
		@NotBlank String unit,
		@Positive @DecimalMax("1") Double powerFactor,
		boolean continuous) {

}
