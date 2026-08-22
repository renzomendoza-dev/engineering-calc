package com.renzoproject.calc_api.mechanical.storage;

/**
 * HTTP response body for a domestic water storage calculation. Mirrors calc-core's
 * {@code DomesticWaterStorageResult} in SI units (L/s, m3) -- matching this endpoint's SI
 * convention, unlike {@link FireWaterStorageResponse}'s GPM/gallons.
 */
public record DomesticWaterStorageResponse(Double resolvedDemandFlowRateLps, Double requiredStorageVolumeM3) {

}
