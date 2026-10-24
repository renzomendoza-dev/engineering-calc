package com.renzoproject.calc_api.electrical.generatorset;

/**
 * A Chapter 7 article a standby source can fall under.
 *
 * @param key             what the request's {@code standbyClass} carries
 * @param label           the system as the PEC names it
 * @param capacityCitation the section that sets its capacity rule
 */
public record StandbyClassOptionDto(String key, String label, String capacityCitation) {

}
