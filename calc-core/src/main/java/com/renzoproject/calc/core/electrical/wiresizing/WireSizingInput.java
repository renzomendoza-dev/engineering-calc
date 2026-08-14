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
 * @param insulationType                     conductor insulation type; determines which
 *                                            {@code AmpacityTable} temperature column applies
 * @param conductorMaterial                  conductor material
 * @param terminationTempRatingCelsius       temperature rating of the terminations/lugs the
 *                                            conductor will land on; must be 60, 75, or 90 —
 *                                            independent of the insulation's own temp rating
 * @param voltageDropCheck                   optional voltage drop cross-check; {@code null}
 *                                            skips it entirely
 * @throws CalculationException if any validation rule above is violated
 */
public record WireSizingInput(
		double loadCurrentAmps,
		boolean isContinuousLoad,
		double ambientTempCelsius,
		int numberOfCurrentCarryingConductors,
		InsulationType insulationType,
		ConductorMaterial conductorMaterial,
		int terminationTempRatingCelsius,
		VoltageDropCheckRequest voltageDropCheck) {

	public WireSizingInput {
		if (loadCurrentAmps <= 0) {
			throw new CalculationException("loadCurrentAmps must be positive");
		}
		if (numberOfCurrentCarryingConductors < 1) {
			throw new CalculationException("numberOfCurrentCarryingConductors must be at least 1");
		}
		if (terminationTempRatingCelsius != 60 && terminationTempRatingCelsius != 75 && terminationTempRatingCelsius != 90) {
			throw new CalculationException("terminationTempRatingCelsius must be 60, 75, or 90");
		}
	}

}
