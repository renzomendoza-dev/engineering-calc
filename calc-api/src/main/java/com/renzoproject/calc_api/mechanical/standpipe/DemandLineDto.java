package com.renzoproject.calc_api.mechanical.standpipe;

/**
 * One step of the demand build-up, mirroring calc-core StandpipeHydraulicResult.DemandLine.
 * Units: gpm.
 */
public record DemandLineDto(String clause, String description, double flowGpm) {

}
