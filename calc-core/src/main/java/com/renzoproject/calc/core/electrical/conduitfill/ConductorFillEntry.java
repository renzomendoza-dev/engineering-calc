package com.renzoproject.calc.core.electrical.conduitfill;

import com.renzoproject.calc.core.electrical.reference.InsulationType;
import com.renzoproject.calc.core.exception.CalculationException;

/**
 * One conductor entry in a conduit fill request: a given insulation type and size, repeated
 * {@code quantity} times.
 */
public record ConductorFillEntry(InsulationType insulationType, String sizeLabel, int quantity) {

	public ConductorFillEntry {
		if (quantity < 1) {
			throw new CalculationException("quantity must be at least 1");
		}
	}

}
