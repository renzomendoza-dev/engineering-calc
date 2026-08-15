package com.renzoproject.calc_api.acoustics;

/**
 * DTO-side mirror of calc-core's {@code AudibilityOutcome}, decoupling the HTTP response
 * contract from calc-core internals -- same pattern as {@code mechanical.pipe}'s
 * {@code FlowRegimeDto}.
 *
 * <p>{@code FAIL} and {@code EXCEEDS_MAX_LIMIT} are both ordinary, successful 200 responses --
 * a fire alarm failing audibility or exceeding the safety ceiling is a valid calculation
 * outcome, not a request error. Only malformed input (e.g. a negative distance) reaches
 * {@code GlobalExceptionHandler} and gets a 400.
 */
public enum AudibilityOutcomeDto {
	PASS,
	FAIL,
	EXCEEDS_MAX_LIMIT
}
