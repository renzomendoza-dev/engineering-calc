package com.renzoproject.calc.core.acoustics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Absolute audibility floor for the {@code sleepingArea} section of
 * {@code reference/acoustics/nfpa72-audibility-thresholds.json}, deserialized directly from that
 * JSON shape. {@code lowFrequencySignalRequiredHz}, {@code lowFrequencyAppliesTo},
 * {@code sourceSection}, and {@code confidence} are present in the JSON but not modeled here --
 * unrelated to the dB pass/fail math -- and skipped via {@code @JsonIgnoreProperties}.
 *
 * @param minimumDbaAtPillow required dB at the pillow, regardless of ambient
 * @param rule                comparison strategy against the relative public/private rule;
 *                            currently only {@code "absoluteFloor_overriddenByRelativeRuleIfGreater"}
 *                            is defined -- callers should validate this rather than assume it
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SleepingAreaThreshold(double minimumDbaAtPillow, String rule) {

}
