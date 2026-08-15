package com.renzoproject.calc_api.smokecontrol;

/** DTO-side discriminator mirroring calc-core's sealed {@code PlumeRegime}. */
public enum PlumeRegimeTypeDto {
	NEAR_FIELD,
	FAR_FIELD
}
