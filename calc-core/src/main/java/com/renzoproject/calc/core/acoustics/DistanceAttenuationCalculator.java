package com.renzoproject.calc.core.acoustics;

import com.renzoproject.calc.core.Calculator;

/**
 * Sound pressure level at a target distance from a known SPL at a reference distance, via the
 * inverse square law ({@link AcousticsMath#distanceAttenuationDb}).
 *
 * <p>A shared building-block for other acoustics calculators in this package (fire alarm
 * audibility, wall/barrier transmission loss — not yet implemented), which are expected to call
 * {@link AcousticsMath#distanceAttenuationDb} directly rather than going through this
 * {@code Calculator<I,R>} wrapper; this class exists for callers that want the full
 * validated-input/result-record shape on its own, not as the only entry point to the math.
 *
 * <p>Single mode, no sealed-interface discriminator — unlike {@code VoltageDropCalculator}'s
 * DC/single-phase/three-phase branching, there's nothing here to branch on.
 */
public class DistanceAttenuationCalculator implements Calculator<DistanceAttenuationInput, DistanceAttenuationResult> {

	@Override
	public DistanceAttenuationResult calculate(DistanceAttenuationInput input) {
		double attenuationDb = AcousticsMath.distanceAttenuationDb(input.referenceDistance(), input.targetDistance());
		double targetSplDb = input.referenceSplDb() + attenuationDb;
		return new DistanceAttenuationResult(targetSplDb, attenuationDb);
	}

}
