package com.renzoproject.calc_api.electrical.reference;

import com.renzoproject.calc.core.electrical.reference.ConductorSize;

/**
 * HTTP response representation of a {@code ConductorSize}, for populating a frontend
 * dropdown of known conductor sizes.
 */
public record ConductorSizeDto(String label, double crossSectionMm2) {

	public static ConductorSizeDto from(ConductorSize size) {
		return new ConductorSizeDto(size.label(), size.crossSectionMm2());
	}

}
