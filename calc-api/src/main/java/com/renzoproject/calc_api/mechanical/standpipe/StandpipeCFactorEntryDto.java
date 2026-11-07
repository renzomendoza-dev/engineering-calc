package com.renzoproject.calc_api.mechanical.standpipe;

/**
 * One row of the Hazen-Williams C table, mirroring calc-core StandpipeCFactorRow.
 */
public record StandpipeCFactorEntryDto(
		String pipeType,
		String label,
		double cFactor,
		String confidence) {

}
