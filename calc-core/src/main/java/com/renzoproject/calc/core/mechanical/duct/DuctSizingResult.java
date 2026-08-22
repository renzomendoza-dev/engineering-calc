package com.renzoproject.calc.core.mechanical.duct;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Speed;

/**
 * Result of {@link DuctSizingCalculator}.
 *
 * @param equivalentDiameter        round-equivalent size, always populated -- for
 *                                   {@link DuctShape#ROUND} this is the actual duct diameter; for
 *                                   {@link DuctShape#RECTANGULAR} this is the ASHRAE
 *                                   equivalent-diameter of the actual solved width/height (which
 *                                   may differ slightly from the target used to seed the solve,
 *                                   due to convergence tolerance)
 * @param ductWidth                  populated only for {@link DuctShape#RECTANGULAR}
 * @param ductHeight                 populated only for {@link DuctShape#RECTANGULAR}
 * @param actualVelocity             the true physical velocity through the actual cross-section
 *                                   ({@code airFlow / area}) -- for RECTANGULAR this is distinct
 *                                   from the velocity a round duct of {@code equivalentDiameter}
 *                                   would have, since the equivalent diameter is a
 *                                   friction-equivalent size, not a literal physical one
 * @param reynoldsNumber             computed at {@code equivalentDiameter}, dimensionless
 * @param frictionFactor             computed at {@code equivalentDiameter}, dimensionless Darcy
 *                                   friction factor
 * @param actualFrictionRatePerMeter the actual achieved friction rate for the resulting duct
 *                                   size/shape -- for EQUAL_FRICTION this should be close to, but
 *                                   not necessarily bit-identical to, the requested target
 */
public record DuctSizingResult(
		Quantity<Length> equivalentDiameter,
		Quantity<Length> ductWidth,
		Quantity<Length> ductHeight,
		Quantity<Speed> actualVelocity,
		double reynoldsNumber,
		double frictionFactor,
		Quantity<Pressure> actualFrictionRatePerMeter) {

}
