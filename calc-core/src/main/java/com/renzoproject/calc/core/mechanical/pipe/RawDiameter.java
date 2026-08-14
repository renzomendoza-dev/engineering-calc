package com.renzoproject.calc.core.mechanical.pipe;

import javax.measure.Quantity;
import javax.measure.quantity.Length;

/**
 * A manually-specified internal diameter, skipping {@link PipeDimensionResolver} entirely —
 * the "custom pipe" escape hatch. Validated (must be positive) by
 * {@link PipeVelocityInput}'s compact constructor, not here.
 */
public record RawDiameter(Quantity<Length> internalDiameter) implements DiameterSpec {

}
