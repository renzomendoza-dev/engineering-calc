package com.renzoproject.calc_api.electrical.generatorset;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Positive;

/**
 * Any other fixed load, mirroring calc-core's {@code GeneratorSetSizingInput.OtherLoad}.
 *
 * @param label               name shown on the load schedule
 * @param voltAmperes         connected load in volt-amperes
 * @param demandFactorPercent demand factor to apply, above 0 and at most 100
 * @param continuous          whether the load runs three hours or more
 */
public record OtherLoadDto(
		String label,
		@Positive double voltAmperes,
		@Positive @DecimalMax("100") double demandFactorPercent,
		boolean continuous) {

}
