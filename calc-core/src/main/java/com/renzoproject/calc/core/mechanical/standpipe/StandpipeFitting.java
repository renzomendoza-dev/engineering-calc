package com.renzoproject.calc.core.mechanical.standpipe;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Fittings of one type on a segment, contributing equivalent pipe length.
 *
 * @param fittingType                key into {@code reference/standpipe/standpipe-reference.json};
 *                                   required unless {@code explicitEquivalentLengthFt} is given
 * @param quantity                   how many; must be positive
 * @param explicitEquivalentLengthFt equivalent length per fitting, ft, used as given (never
 *                                   rescaled); for a hose valve whose manufacturer publishes its loss
 *                                   (10.2.6.1.1). Must be positive when given
 */
public record StandpipeFitting(
		String fittingType,
		int quantity,
		Double explicitEquivalentLengthFt) {

	public StandpipeFitting {
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
