package com.renzoproject.calc.core.electrical.motorflc;

import com.renzoproject.calc.core.electrical.reference.MotorClass;
import com.renzoproject.calc.core.electrical.reference.MotorPhaseType;
import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Input parameters for {@link MotorFlcCalculator}.
 *
 * @param phaseType                      motor supply type
 * @param motorClass                     required and non-null when {@code phaseType} is
 *                                        {@link MotorPhaseType#THREE_PHASE}; must be
 *                                        {@code null} otherwise — only three-phase data
 *                                        distinguishes induction vs. synchronous
 * @param horsepowerLabel                horsepower label matching a PEC table's size
 *                                        notation exactly (e.g. {@code "1/4"}, {@code "1 1/2"})
 * @param voltage                        motor voltage
 * @param synchronousPowerFactorPercent  nullable; only meaningful when {@code motorClass} is
 *                                        {@link MotorClass#SYNCHRONOUS} — must be {@code null}
 *                                        otherwise
 * @throws CalculationException if any validation rule above is violated
 */
public record MotorFlcInput(
		MotorPhaseType phaseType,
		MotorClass motorClass,
		String horsepowerLabel,
		int voltage,
		Integer synchronousPowerFactorPercent) {

	public MotorFlcInput {
		if (phaseType == MotorPhaseType.THREE_PHASE) {
			if (motorClass == null) {
				throw new CalculationException("motorClass is required when phaseType is THREE_PHASE");
			}
		} else if (motorClass != null) {
			throw new CalculationException("motorClass must be null when phaseType is not THREE_PHASE");
		}
		if (synchronousPowerFactorPercent != null && motorClass != MotorClass.SYNCHRONOUS) {
			throw new CalculationException("synchronousPowerFactorPercent is only valid when motorClass is SYNCHRONOUS");
		}
	}

}
