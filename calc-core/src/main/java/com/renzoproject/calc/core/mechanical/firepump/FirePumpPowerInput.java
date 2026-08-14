package com.renzoproject.calc.core.mechanical.firepump;

/**
 * Two ways to specify what {@link FirePumpPowerCalculator} should evaluate — mirrors
 * {@code mechanical.pipe}'s {@code DiameterSpec} pattern: a sealed interface rather than one
 * record with optional fields, since the two cases need genuinely different data
 * ({@link RatedPointOnly} doesn't require a curve at all).
 */
public sealed interface FirePumpPowerInput permits RatedPointOnly, FullCurve {

}
