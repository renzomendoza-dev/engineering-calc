package com.renzoproject.calc_api.mechanical.reference;

import com.renzoproject.calc.core.mechanical.pipe.PipeSizeReference;

/**
 * HTTP response representation of a {@code PipeSizeReference}, for populating a frontend
 * nominal-size dropdown.
 *
 * <p>{@code nominalSize} (not {@code nominalLabel}) is the value to submit back as the
 * pipe-velocity request's {@code nominalLabel} field — see {@code NominalSize}'s Javadoc in
 * calc-core for this naming gotcha.
 */
public record PipeSizeDto(String nominalSize, String nominalLabel, double internalDiameterMm) {

	public static PipeSizeDto from(PipeSizeReference size) {
		return new PipeSizeDto(size.nominalSize(), size.nominalLabel(), size.internalDiameterMm());
	}

}
