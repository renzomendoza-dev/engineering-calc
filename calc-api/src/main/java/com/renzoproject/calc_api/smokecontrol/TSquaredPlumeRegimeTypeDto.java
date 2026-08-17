package com.renzoproject.calc_api.smokecontrol;

/**
 * DTO-side discriminator mirroring calc-core's sealed {@code TSquaredPlumeRegime}. Deliberate
 * duplicate of {@link PlumeRegimeTypeDto} -- kept separate to match the "fully separate,
 * duplicated" decision already made at the core layer, not shared between the two endpoints.
 */
public enum TSquaredPlumeRegimeTypeDto {
	NEAR_FIELD,
	FAR_FIELD
}
