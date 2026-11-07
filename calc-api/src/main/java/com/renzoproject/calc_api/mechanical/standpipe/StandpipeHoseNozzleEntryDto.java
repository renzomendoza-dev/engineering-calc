package com.renzoproject.calc_api.mechanical.standpipe;

/**
 * One row of NFPA 14 Table A.10.2.6, mirroring calc-core StandpipeHoseNozzleRow. {@code nozzle} and
 * {@code sourceNote} may be null.
 */
public record StandpipeHoseNozzleEntryDto(
		int number,
		String label,
		String hose,
		String nozzle,
		double outletPressurePsi,
		double flowGpm,
		boolean aboveMinimumResidual,
		String confidence,
		String sourceNote) {

}
