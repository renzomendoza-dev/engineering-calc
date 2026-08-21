package com.renzoproject.calc.core.electrical.motorflc;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.electrical.wiresizing.WireSizingCalculator;
import com.renzoproject.calc.core.electrical.wiresizing.WireSizingInput;
import com.renzoproject.calc.core.electrical.wiresizing.WireSizingResult;

/**
 * Chains {@link MotorFlcCalculator} into {@link WireSizingCalculator}: given a motor's
 * horsepower/voltage, resolves its full-load current and then recommends a branch-circuit
 * conductor size for it in one step, so callers don't have to manually copy the FLC result
 * into a separate wire sizing request.
 *
 * <p>Both sub-calculators are plainly instantiated, matching the pattern already used
 * throughout this codebase (calc-core has no Spring dependency, so there's no DI to reach for
 * here).
 */
public class MotorConductorSizingCalculator implements Calculator<MotorConductorSizingInput, MotorConductorSizingResult> {

	private final MotorFlcCalculator motorFlcCalculator = new MotorFlcCalculator();
	private final WireSizingCalculator wireSizingCalculator = new WireSizingCalculator();

	@Override
	public MotorConductorSizingResult calculate(MotorConductorSizingInput input) {
		MotorFlcInput motorFlcInput = new MotorFlcInput(
				input.phaseType(),
				input.motorClass(),
				input.horsepowerLabel(),
				input.voltage(),
				input.synchronousPowerFactorPercent());
		MotorFlcResult motorFlcResult = motorFlcCalculator.calculate(motorFlcInput);

		// isContinuousLoad is always true here — not a pass-through option — because PEC
		// 4.30.2 independently requires branch-circuit conductors for a single continuous-duty
		// motor to be sized at not less than 125% of FLC. This reuses WireSizingCalculator's
		// existing isContinuousLoad flag (originally built for the general Article 2.10/2.15
		// continuous-load rule) purely because both rules apply the same 125% multiplier — it
		// is NOT the same code basis, just a numerically identical one.
		//
		// loadCurrentAmps below is deliberately the RAW motorFlcResult.flcAmps(), not
		// motorFlcResult.minimumConductorAmpacity() (which is already flcAmps * 1.25).
		// WireSizingCalculator applies its own 125% via isContinuousLoad, so passing the
		// already-multiplied value here would double-apply the factor. Do not "fix" this by
		// pre-multiplying loadCurrentAmps — that is the exact bug this comment exists to
		// prevent.
		WireSizingInput wireSizingInput = new WireSizingInput(
				motorFlcResult.flcAmps(),
				true,
				input.ambientTempCelsius(),
				input.numberOfCurrentCarryingConductors(),
				input.numberOfParallelSets(),
				input.insulationType(),
				input.conductorMaterial(),
				input.terminationTempRatingCelsius(),
				input.voltageDropCheck());
		WireSizingResult wireSizingResult = wireSizingCalculator.calculate(wireSizingInput);

		return new MotorConductorSizingResult(motorFlcResult, wireSizingResult);
	}

}
