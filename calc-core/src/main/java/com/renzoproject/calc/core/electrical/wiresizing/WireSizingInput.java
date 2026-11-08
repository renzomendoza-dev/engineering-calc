package com.renzoproject.calc.core.electrical.wiresizing;

import com.renzoproject.calc.core.electrical.reference.ConductorMaterial;
import com.renzoproject.calc.core.electrical.reference.InsulationType;
import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Input parameters for {@link WireSizingCalculator}.
 *
 * @param loadCurrentAmps                    load current, in amperes; must be positive
 * @param isContinuousLoad                   whether the load runs 3+ hours continuously; if
 *                                            {@code true}, the required ampacity is 125% of
 *                                            {@code loadCurrentAmps}
 * @param ambientTempCelsius                 ambient temperature the conductors will run
 *                                            through
 * @param numberOfCurrentCarryingConductors  current-carrying conductors sharing the same
 *                                            raceway/cable; must be at least 1
 * @param numberOfParallelSets               how many identical conductors run in parallel to
 *                                            carry this one circuit's current, per PEC/NEC
 *                                            310.10(H) (e.g. 2 conductors, each rated for half
 *                                            the load, instead of one conductor too large to be
 *                                            practical); must be at least 1. {@code 1} is the
 *                                            ordinary single-conductor case. Independent of
 *                                            {@code voltageDropCheck}'s own
 *                                            {@code parallelSetsPerPhase} — that field feeds a
 *                                            separate voltage-drop calculation and is not
 *                                            derived from this one, so a caller running a
 *                                            voltage drop check on a split run is responsible
 *                                            for passing a matching value there too.
 * @param insulationType                     conductor insulation type; determines which
 *                                            {@code AmpacityTable} temperature column applies
 * @param conductorMaterial                  conductor material
 * @param terminationTempRatingCelsius       temperature rating of the terminations/lugs the
 *                                            conductor will land on; must be 60, 75, or 90 —
 *                                            independent of the insulation's own temp rating
 * @param voltageDropCheck                   optional voltage drop cross-check; {@code null}
 *                                            skips it entirely
 * @param overcurrentDeviceAmps              optional rating of the overcurrent device that will protect
 *                                            the conductor, A; must be positive when given. A size
 *                                            whose small-conductor limit (PEC 2.40.1.4(D)) is below
 *                                            it is passed over for the next one up. {@code null} sizes
 *                                            by ampacity alone and only reports the limit
 * @throws CalculationException if any validation rule above is violated
 */
public record WireSizingInput(
		double loadCurrentAmps,
		boolean isContinuousLoad,
		double ambientTempCelsius,
		int numberOfCurrentCarryingConductors,
		int numberOfParallelSets,
		InsulationType insulationType,
		ConductorMaterial conductorMaterial,
		int terminationTempRatingCelsius,
		VoltageDropCheckRequest voltageDropCheck,
		Double overcurrentDeviceAmps) {

	/** The input without a stated overcurrent device: sized by ampacity alone. */
	public WireSizingInput(
			double loadCurrentAmps,
			boolean isContinuousLoad,
			double ambientTempCelsius,
			int numberOfCurrentCarryingConductors,
			int numberOfParallelSets,
			InsulationType insulationType,
			ConductorMaterial conductorMaterial,
			int terminationTempRatingCelsius,
			VoltageDropCheckRequest voltageDropCheck) {
		this(loadCurrentAmps, isContinuousLoad, ambientTempCelsius, numberOfCurrentCarryingConductors, numberOfParallelSets,
				insulationType, conductorMaterial, terminationTempRatingCelsius, voltageDropCheck, null);
	}

	public WireSizingInput {
		if (overcurrentDeviceAmps != null && overcurrentDeviceAmps <= 0) {
			throw new CalculationException("overcurrentDeviceAmps must be positive");
		}
		if (loadCurrentAmps <= 0) {
			throw new CalculationException("loadCurrentAmps must be positive");
		}
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
