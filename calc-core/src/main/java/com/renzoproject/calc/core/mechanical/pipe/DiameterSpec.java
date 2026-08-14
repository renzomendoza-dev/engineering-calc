package com.renzoproject.calc.core.mechanical.pipe;

/**
 * How a pipe's internal diameter is specified for {@link PipeVelocityCalculator}. A sealed
 * interface rather than one record with optional fields, since the two cases are genuinely
 * different inputs (a catalog lookup vs. a manual value) — callers pattern-match with a switch.
 */
public sealed interface DiameterSpec permits NominalSize, RawDiameter {

}
