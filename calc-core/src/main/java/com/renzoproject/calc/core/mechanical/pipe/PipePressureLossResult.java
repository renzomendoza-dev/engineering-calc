package com.renzoproject.calc.core.mechanical.pipe;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Speed;

/**
 * @param reynoldsNumber            dimensionless — plain {@code double}, not an Indriya quantity
 * @param transitionalRegimeWarning {@code true} if {@code 2300 < reynoldsNumber < 4000} — the
 *                                  calculation still completes using the turbulent correlation
 *                                  (documented engineering practice), this just flags that the
 *                                  regime is genuinely ambiguous
 * @param frictionFactor            dimensionless Darcy friction factor
 */
public record PipePressureLossResult(
		Quantity<Speed> velocity,
		double reynoldsNumber,
		FlowRegime flowRegime,
		boolean transitionalRegimeWarning,
		double frictionFactor,
		Quantity<Length> headLoss,
		Quantity<Pressure> pressureLoss) {

}
