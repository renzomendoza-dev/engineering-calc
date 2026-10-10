package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Fittings of one type on a pipe segment, contributing equivalent pipe length.
 *
 * @param fittingType               key into {@code reference/sprinkler/fitting-equivalent-lengths.json}
 *                                  (e.g. {@code ELBOW_90_STANDARD}); required unless
 *                                  {@code explicitEquivalentLengthFt} is given
 * @param quantity                  how many of this fitting; must be positive
 * @param explicitEquivalentLengthFt caller-supplied equivalent length per fitting, in feet. When
 *                                  present it is used as-is and no reference lookup happens --
 *                                  the escape hatch for anyone who doesn't have the published
 *                                  table loaded. Must be positive if given
 * @throws CalculationException if any rule above is violated
 */
public record SegmentFitting(
		String fittingType,
		int quantity,
		Double explicitEquivalentLengthFt) {

	public SegmentFitting {
		if (quantity <= 0) {
			throw new CalculationException("fitting quantity must be positive (was " + quantity + ")");
		}
		boolean hasType = fittingType != null && !fittingType.isBlank();
		if (!hasType && explicitEquivalentLengthFt == null) {
			throw new CalculationException("a fitting needs either a fittingType or an explicitEquivalentLengthFt");
		}
		if (explicitEquivalentLengthFt != null && explicitEquivalentLengthFt <= 0) {
			throw new CalculationException("explicitEquivalentLengthFt must be positive when given");
		}
	}

}
