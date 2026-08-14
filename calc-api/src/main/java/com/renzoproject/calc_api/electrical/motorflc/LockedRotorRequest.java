package com.renzoproject.calc_api.electrical.motorflc;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * HTTP request body for a standalone locked-rotor current lookup.
 */
public record LockedRotorRequest(boolean isPolyphase, @NotBlank String horsepowerLabel, @Positive int voltage) {

}
