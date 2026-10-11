package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * A water supply described by a flow test: static pressure, and a residual pressure measured at a
 * known flow. Pressure available at any other flow follows the standard 1.85-exponent supply
 * curve, {@code P(Q) = Ps - (Ps - Pr) * (Q/Qr)^1.85}.
 *
 * <p><b>Units:</b> psi and US gpm.
 *
 * <p>{@code P(0)} returns the static pressure and {@code P(Qr)} the residual pressure exactly.
 * Flows beyond {@code Qr} extrapolate along the same curve, which is normal practice, but the
 * curve will eventually cross zero -- a demand that far out of range is a design problem, not a
 * math problem, so it's reported through the result's safety margin rather than thrown here.
 *
 * @param staticPressurePsi   no-flow pressure
 * @param residualPressurePsi pressure measured at {@code flowAtResidualGpm}; must be less than
 *                            {@code staticPressurePsi}
 * @param flowAtResidualGpm   flow at which the residual pressure was measured; must be positive
 * @throws CalculationException if the test data isn't a usable curve
 */
public record SupplyCurve(
		double staticPressurePsi,
		double residualPressurePsi,
		double flowAtResidualGpm) implements WaterSupply {

	private static final double FLOW_EXPONENT = 1.85;

	public SupplyCurve {
		if (flowAtResidualGpm <= 0) {
			throw new CalculationException("flowAtResidualGpm must be positive (was " + flowAtResidualGpm + ")");
		}
		if (staticPressurePsi <= 0) {
			throw new CalculationException("staticPressurePsi must be positive (was " + staticPressurePsi + ")");
		}
		if (residualPressurePsi < 0) {
			throw new CalculationException("residualPressurePsi must not be negative (was " + residualPressurePsi + ")");
		}
		if (residualPressurePsi >= staticPressurePsi) {
			throw new CalculationException("residualPressurePsi (" + residualPressurePsi + ") must be less than "
					+ "staticPressurePsi (" + staticPressurePsi + ") -- a flow test always loses pressure as flow increases");
		}
	}

	/**
	 * @param flowGpm flow to evaluate the curve at; must not be negative
	 * @return pressure the supply can deliver at that flow, psi (can go negative far past
	 *         {@code flowAtResidualGpm}, meaning the supply cannot deliver that flow at all)
	 * @throws CalculationException if {@code flowGpm} is negative
	 */
	public double availablePressurePsi(double flowGpm) {
		if (flowGpm < 0) {
			throw new CalculationException("flowGpm must not be negative (was " + flowGpm + ")");
		}
		double drop = (staticPressurePsi - residualPressurePsi) * Math.pow(flowGpm / flowAtResidualGpm, FLOW_EXPONENT);
		return staticPressurePsi - drop;
	}

}
