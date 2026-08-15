package com.renzoproject.calc.core.acoustics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Audibility offset rule for a {@code notificationModes.public}/{@code .private} section of
 * {@code reference/acoustics/nfpa72-audibility-thresholds.json}. Deserialized directly from that
 * JSON shape (mirrors {@code ConductorImpedanceEntry}'s dual role as both parse target and
 * consumed type); {@code sourceSection}, {@code measurementHeightMeters}, {@code confidence}, and
 * {@code notes} are present in the JSON but deliberately not modeled here -- transcription
 * bookkeeping only, not needed by {@link com.renzoproject.calc.core.acoustics.FireAlarmAudibilityCalculator}
 * -- and skipped harmlessly via {@code @JsonIgnoreProperties}.
 *
 * @param aboveAverageAmbientDb required dB above the measured average ambient sound level
 * @param aboveMaxSustainedDb   required dB above the measured 60-second-max sustained ambient
 *                              sound level
 * @param rule                  comparison strategy for the two candidates above; currently only
 *                              {@code "greaterOf"} is defined -- callers should validate this
 *                              rather than assume it, so a future JSON edit to an unrecognized
 *                              rule fails loudly instead of silently mis-computing
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificationModeThreshold(double aboveAverageAmbientDb, double aboveMaxSustainedDb, String rule) {

}
