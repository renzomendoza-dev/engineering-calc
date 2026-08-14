package com.renzoproject.calc.core.electrical.conduitfill;

import com.renzoproject.calc.core.electrical.reference.ConduitType;
import com.renzoproject.calc.core.exception.CalculationException;

import java.util.List;

public record ConduitFillInput(List<ConductorFillEntry> conductors, ConduitType conduitType) {

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
