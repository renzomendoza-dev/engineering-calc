package com.renzoproject.calc.core.acoustics;

/**
 * Result of {@link DistanceAttenuationCalculator}. Deliberately doesn't echo the input fields
 * (matching every other calc-core {@code Result} record — none of them echo their inputs; a
 * caller already has the {@code Input} instance it built, in scope alongside this result).
 *
 * @param targetSplDb   calculated sound pressure level at the target distance, dB
 * @param attenuationDb signed dB delta added to {@code referenceSplDb} to get
 *                      {@code targetSplDb} — see {@link AcousticsMath#distanceAttenuationDb}
 *                      for the sign convention
 */
public record DistanceAttenuationResult(double targetSplDb, double attenuationDb) {

}
