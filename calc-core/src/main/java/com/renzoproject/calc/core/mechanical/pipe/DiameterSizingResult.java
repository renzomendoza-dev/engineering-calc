package com.renzoproject.calc.core.mechanical.pipe;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Speed;

/**
 * @param calculatedMinDiameter  raw geometric result, pre-rounding:
 *                               {@code D = sqrt(4Q / (pi * targetVelocity))}
 * @param nominalPipeSize        resolved nominal label from {@link PipeDimensionResolver}, e.g.
 *                               {@code "2\" (DN50)"}
 * @param actualInternalDiameter the resolved size's real internal diameter
 * @param actualVelocity         velocity recomputed using {@code actualInternalDiameter};
 *                               guaranteed {@code <=} the requested target velocity
 */
public record DiameterSizingResult(
		Quantity<Length> calculatedMinDiameter,
		String nominalPipeSize,
		Quantity<Length> actualInternalDiameter,
		Quantity<Speed> actualVelocity) implements PipeSizingResult {

}
