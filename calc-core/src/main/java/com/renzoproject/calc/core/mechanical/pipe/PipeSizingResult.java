package com.renzoproject.calc.core.mechanical.pipe;

/**
 * Result of {@link PipeVelocityCalculator}. A sealed interface rather than one flat record with
 * nullable fields, since the two modes produce genuinely different shapes — callers
 * pattern-match with a switch expression.
 */
public sealed interface PipeSizingResult permits VelocityResult, DiameterSizingResult {

}
