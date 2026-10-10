package com.renzoproject.calc_api.mechanical.sprinkler;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Water supply flow test. The "residual below static" rule is enforced by calc-core's
 * {@code SupplyCurve}, not duplicated here.
 *
 * @param staticPressurePsi   no-flow pressure
 * @param residualPressurePsi pressure measured at {@code flowAtResidualGpm}
 * @param flowAtResidualGpm   flow at which the residual pressure was measured
 */
public record WaterSupplyDto(
		@NotNull @Positive Double staticPressurePsi,
		@NotNull @PositiveOrZero Double residualPressurePsi,
		@NotNull @Positive Double flowAtResidualGpm) {

}
