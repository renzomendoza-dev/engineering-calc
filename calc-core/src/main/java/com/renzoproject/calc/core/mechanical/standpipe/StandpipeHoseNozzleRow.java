package com.renzoproject.calc.core.mechanical.standpipe;

/**
 * One row of NFPA 14 Table A.10.2.6, a hose and nozzle combination fire departments use, with the
 * pressure needed at the hose valve outlet to flow it. A default a form can offer for a hose
 * connection's design residual pressure; the fire department should flow test its own choice.
 *
 * @param number               the table's calculation number, 1 to 7
 * @param label                one line naming the combination
 * @param hose                 the hose as printed
 * @param nozzle               the nozzle as printed; null where the table gives none (row 7)
 * @param outletPressurePsi    pressure needed at the hose valve outlet, psi
 * @param flowGpm              flow the combination gives at that pressure, gpm
 * @param aboveMinimumResidual the table's asterisk: it needs more than the minimum required residual
 * @param confidence           {@code verified} or {@code verify}
 * @param sourceNote           what to know about this row's transcription; null when there is nothing to add
 */
public record StandpipeHoseNozzleRow(
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
