package com.renzoproject.calc.core.electrical.motorflc;

import com.renzoproject.calc.core.electrical.reference.ConductorMaterial;
import com.renzoproject.calc.core.electrical.reference.InsulationType;
import com.renzoproject.calc.core.electrical.reference.MotorClass;
import com.renzoproject.calc.core.electrical.reference.MotorPhaseType;
import com.renzoproject.calc.core.electrical.wiresizing.VoltageDropCheckRequest;
import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Combined input for {@link MotorConductorSizingCalculator}: the motor-identification fields
 * {@code MotorFlcInput} needs, plus the wire sizing condition fields {@code WireSizingInput}
 * needs — minus {@code loadCurrentAmps} and {@code isContinuousLoad}, since both are derived
 * from the FLC calculation rather than supplied directly (see the calculator).
 *
 * @param phaseType                          motor supply type
 * @param motorClass                         required and non-null when {@code phaseType} is
 *                                            {@link MotorPhaseType#THREE_PHASE}; must be
 *                                            {@code null} otherwise
 * @param horsepowerLabel                    horsepower label matching a PEC table's size
 *                                            notation exactly (e.g. {@code "1/4"},
 *                                            {@code "1 1/2"})
 * @param voltage                            motor voltage
 * @param synchronousPowerFactorPercent      nullable; only meaningful when {@code motorClass}
 *                                            is {@link MotorClass#SYNCHRONOUS}
 * @param ambientTempCelsius                 ambient temperature the conductors will run
 *                                            through
 * @param numberOfCurrentCarryingConductors  current-carrying conductors sharing the same
 *                                            raceway/cable; must be at least 1
 * @param numberOfParallelSets               how many identical conductors run in parallel to
 *                                            carry this motor's current; must be at least 1 —
 *                                            see {@code WireSizingInput}'s Javadoc for the full
 *                                            explanation
 * @param insulationType                     conductor insulation type
 * @param conductorMaterial                  conductor material
 * @param terminationTempRatingCelsius       must be 60, 75, or 90
 * @param voltageDropCheck                   optional voltage drop cross-check; {@code null}
 *                                            skips it entirely
 * @throws CalculationException if any validation rule above is violated
 */
public record MotorConductorSizingInput(
		MotorPhaseType phaseType,
		MotorClass motorClass,
		String horsepowerLabel,
		int voltage,
		Integer synchronousPowerFactorPercent,
		double ambientTempCelsius,
		int numberOfCurrentCarryingConductors,
		int numberOfParallelSets,
		InsulationType insulationType,
		ConductorMaterial conductorMaterial,
		int terminationTempRatingCelsius,
		VoltageDropCheckRequest voltageDropCheck) {

	public MotorConductorSizingInput {
		// Delegates the motor-identification fields to MotorFlcInput's own compact
		// constructor, so this stays in sync with MotorFlcInput's validation automatically
		// (same rules, same messages) instead of duplicating that logic here. The constructed
		// instance is discarded — it exists only to trigger validation via side effect.
		new MotorFlcInput(phaseType, motorClass, horsepowerLabel, voltage, synchronousPowerFactorPercent);

		// WireSizingInput's compact constructor also validates loadCurrentAmps (must be
		// positive), but that field doesn't exist yet at this stage — it's derived later from
		// the FLC result (see MotorConductorSizingCalculator). Building a placeholder
		// WireSizingInput here just to borrow its validation would mean faking a
		// loadCurrentAmps value, which is worse than duplicating these three simple checks
		// (none depend on loadCurrentAmps) with matching messages.
		if (numberOfCurrentCarryingConductors < 1) {
			throw new CalculationException("numberOfCurrentCarryingConductors must be at least 1");
		}
		if (numberOfParallelSets < 1) {
			throw new CalculationException("numberOfParallelSets must be at least 1");
		}
		if (terminationTempRatingCelsius != 60 && terminationTempRatingCelsius != 75 && terminationTempRatingCelsius != 90) {
			throw new CalculationException("terminationTempRatingCelsius must be 60, 75, or 90");
		}
	}

}
