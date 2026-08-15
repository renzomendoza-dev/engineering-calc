package com.renzoproject.calc_api.acoustics;

/**
 * DTO-side mirror of calc-core's {@code GoverningAudibilityRule}, decoupling the HTTP response
 * contract from calc-core internals -- same pattern as {@code mechanical.pipe}'s
 * {@code FlowRegimeDto}.
 */
public enum GoverningAudibilityRuleDto {
	AVERAGE_AMBIENT_PLUS_OFFSET,
	MAX_SUSTAINED_PLUS_OFFSET,
	ABSOLUTE_SLEEPING_FLOOR
}
