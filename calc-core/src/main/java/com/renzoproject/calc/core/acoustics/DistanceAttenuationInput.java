package com.renzoproject.calc.core.acoustics;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Input for {@link DistanceAttenuationCalculator}.
 *
 * @param referenceSplDb    known sound pressure level at {@code referenceDistance}, dB
 * @param referenceDistance distance the reference SPL was measured/specified at, meters; must
 *                          be positive
 * @param targetDistance    distance to calculate the SPL at, meters; must be positive. May be
 *                          greater or less than {@code referenceDistance} — the inverse square
 *                          law formula is symmetric either direction (attenuation moving away,
 *                          amplification moving closer)
 * @throws CalculationException if either distance is not positive
 */
public record DistanceAttenuationInput(double referenceSplDb, double referenceDistance, double targetDistance) {

	public DistanceAttenuationInput {
		if (referenceDistance <= 0) {
			throw new CalculationException("referenceDistance must be positive");
		}
		if (targetDistance <= 0) {
			throw new CalculationException("targetDistance must be positive");
		}
	}

}
