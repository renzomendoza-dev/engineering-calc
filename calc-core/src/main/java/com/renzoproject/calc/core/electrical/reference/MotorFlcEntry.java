package com.renzoproject.calc.core.electrical.reference;

/**
 * One row from a PEC Article 4.30 full-load current table (4.30.14.1 through 4.30.14.4).
 *
 * @param motorClass nullable — only populated for {@link MotorPhaseType#THREE_PHASE} entries
 */
public record MotorFlcEntry(
		MotorPhaseType phaseType,
		MotorClass motorClass,
		String sizeLabel,
		int voltage,
		double flcAmps) {

}
