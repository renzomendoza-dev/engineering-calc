package com.renzoproject.calc_api.acoustics;

/**
 * HTTP response body for a distance attenuation calculation. Mirrors calc-core's
 * {@code DistanceAttenuationResult} exactly — no echoed request fields, consistent with every
 * other calc-api response DTO in this codebase (e.g. {@code FirePumpCapacityResponse}), none of
 * which echo their request's inputs either.
 */
public record DistanceAttenuationResponse(Double targetSplDb, Double attenuationDb) {

}
