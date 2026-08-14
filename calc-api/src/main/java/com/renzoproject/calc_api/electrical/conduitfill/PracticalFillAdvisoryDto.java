package com.renzoproject.calc_api.electrical.conduitfill;

import com.renzoproject.calc.core.electrical.conduitfill.PracticalFillAdvisory;

/**
 * HTTP response representation of calc-core's {@link PracticalFillAdvisory} — a field-
 * experience pull-ease heuristic, kept as its own nested object on {@link ConduitFillResponse}
 * rather than flattened into its fields, so it stays visibly distinct from the PEC legal fill
 * compliance result it sits alongside. This is explicitly NOT a code requirement.
 */
public record PracticalFillAdvisoryDto(boolean mayBeDifficultToPull, String note) {

	public static PracticalFillAdvisoryDto from(PracticalFillAdvisory advisory) {
		return new PracticalFillAdvisoryDto(advisory.mayBeDifficultToPull(), advisory.note());
	}

}
