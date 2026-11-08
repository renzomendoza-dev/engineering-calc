package com.renzoproject.calc.core.electrical.conduitfill;

import com.renzoproject.calc.core.electrical.reference.ConduitType;
import com.renzoproject.calc.core.exception.CalculationException;

import java.util.List;

/**
 * @param conductors  the conductors to fit; not empty
 * @param conduitType the conduit or tubing type
 * @param nipple      whether the run is a nipple of not more than 600 mm between boxes, cabinets or similar
 *                    enclosures, which PEC Chapter 10 note (4) lets be filled to 60 percent of its area
 */
public record ConduitFillInput(List<ConductorFillEntry> conductors, ConduitType conduitType, boolean nipple) {

	/** A run that is not a nipple: Table 10.1.1.1 applies. */
	public ConduitFillInput(List<ConductorFillEntry> conductors, ConduitType conduitType) {
		this(conductors, conduitType, false);
	}

	public ConduitFillInput {
		if (conductors == null || conductors.isEmpty()) {
			throw new CalculationException("conductors must not be null or empty");
		}
		if (conduitType == null) {
			throw new CalculationException("conduitType must not be null");
		}
		conductors = List.copyOf(conductors);
	}

}
