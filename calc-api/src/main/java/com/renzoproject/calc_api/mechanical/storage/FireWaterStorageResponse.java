package com.renzoproject.calc_api.mechanical.storage;

/**
 * HTTP response body for a fire water storage calculation. Mirrors calc-core's
 * {@code FireWaterStorageResult} exactly, in gallons -- matching this endpoint's GPM/gallons
 * convention, unlike {@link DomesticWaterStorageResponse}'s SI units.
 */
public record FireWaterStorageResponse(
		Double resolvedDurationMinutesMin,
		Double resolvedDurationMinutesMax,
		Double durationMinutesUsed,
		boolean usedConservativeDefault,
		Double requiredStorageVolumeGallons) {

}
