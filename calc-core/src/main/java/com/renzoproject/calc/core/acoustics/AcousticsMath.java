package com.renzoproject.calc.core.acoustics;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Shared math for acoustics calculators in this package. Deliberately minimal — one method for
 * now; logarithmic summation, NC/RC curve helpers, etc. are deferred to later calculators that
 * actually need them, not pre-built speculatively here.
 *
 * <p>Sound pressure level (dB) is a dimensionless logarithmic ratio, not a physical quantity
 * Indriya's unit/quantity system fits naturally — there's no SI base unit for "decibel" the way
 * there is for length or pressure, and a dB value's meaning depends entirely on its reference,
 * which {@code Quantity}/{@code Unit} isn't designed to carry. Plain {@code double} is used
 * throughout this package instead, consistent with how this codebase already treats other
 * dimensionless calculated values (e.g. friction factor and Reynolds number in
 * {@code mechanical.pipe.PipePressureLossResult}).
 */
public final class AcousticsMath {

	private AcousticsMath() {
	}

	/**
	 * Inverse square law distance attenuation.
	 *
	 * <p><b>Sign convention:</b> negative when {@code d2 > d1} (moving away — level drops) and
	 * positive when {@code d2 < d1} (moving closer — level rises), so a caller can get the SPL
	 * at {@code d2} with simple addition: {@code splAtD2 = splAtD1 + distanceAttenuationDb(d1, d2)}.
	 * Implemented as {@code 20 * log10(d1 / d2)} — the {@code d1/d2} ratio order (not
	 * {@code d2/d1}) is what actually produces this sign behavior; algebraically it's identical
	 * to the textbook {@code splAtD2 = splAtD1 - 20*log10(d2/d1)} form
	 * (since {@code -log10(x) = log10(1/x)}), just rearranged so the return value is a
	 * ready-to-add signed delta instead of a term the caller must remember to subtract — the
	 * whole point being to make the sign impossible for a downstream caller to get backwards.
	 *
	 * <p>Validated here (not only by {@link DistanceAttenuationInput}) because this method is
	 * meant to be called directly by other acoustics calculators (fire alarm audibility,
	 * wall/barrier transmission loss) that may never construct a
	 * {@link DistanceAttenuationInput} at all — this is the only guard those callers get.
	 *
	 * @param d1 reference distance, meters; must be positive
	 * @param d2 target distance, meters; must be positive
	 * @return signed dB delta; magnitude is {@code ~6.02} ({@code 20*log10(2)}) for a doubling
	 *         or halving of distance — the well-known "6 dB per doubling" rule of thumb
	 * @throws CalculationException if either distance is not positive
	 */
	public static double distanceAttenuationDb(double d1, double d2) {
		if (d1 <= 0) {
			throw new CalculationException("d1 must be positive");
		}
		if (d2 <= 0) {
			throw new CalculationException("d2 must be positive");
		}
		return 20.0 * Math.log10(d1 / d2);
	}

}
