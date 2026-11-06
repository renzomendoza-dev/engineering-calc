package com.renzoproject.calc_api.mechanical.standpipe;

/**
 * One worksheet row, mirroring calc-core StandpipeHydraulicResult.SegmentRow. <b>Units:</b> psi,
 * gpm, ft, in., ft/s. {@code role} and {@code hoseConnectionSize} are the request's values.
 */
public record StandpipeSegmentRowDto(
		String label,
		String role,
		String hoseConnectionSize,
		String nominalSize,
		double insideDiameterInch,
		double cFactor,
		double flowAddedGpm,
		double flowGpm,
		double velocityFtPerSec,
		double pipeLengthFt,
		double fittingsLengthFt,
		double frictionPsiPerFt,
		double frictionLossPsi,
		double elevationRiseFt,
		double elevationPsi,
		double pressureAtRemoteEndPsi,
		double pressureAtSupplyEndPsi,
		boolean residualRaised) {

}
