package com.renzoproject.calc_api.mechanical.sprinkler;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * A water supply described by a flow test -- a city main, or any source with a measured static and
 * residual. The "residual below static" rule is enforced by calc-core's {@code SupplyCurve}, not
 * duplicated here.
 *
 * @param staticPressurePsi   no-flow pressure
 * @param residualPressurePsi pressure measured at {@code flowAtResidualGpm}
 * @param flowAtResidualGpm   flow at which the residual pressure was measured
 */
public record FlowTestSupplyDto(
		@NotNull @Positive Double staticPressurePsi,
		@NotNull @PositiveOrZero Double residualPressurePsi,
		@NotNull @Positive Double flowAtResidualGpm) {

}
