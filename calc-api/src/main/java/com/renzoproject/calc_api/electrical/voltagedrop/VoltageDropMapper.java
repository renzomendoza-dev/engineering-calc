package com.renzoproject.calc_api.electrical.voltagedrop;

import com.renzoproject.calc.core.electrical.voltagedrop.VoltageDropInput;
import com.renzoproject.calc.core.electrical.voltagedrop.VoltageDropResult;

/**
 * Maps between calc-api's voltage drop DTOs and calc-core's calculator types.
 */
public final class VoltageDropMapper {

	private VoltageDropMapper() {
	}

	/**
	 * @param resolvedResistanceOhmsPerMeter resistance already resolved by the caller —
	 *                                        either from PEC reference tables or the
	 *                                        request's custom value
	 * @param resolvedReactanceOhmsPerMeter  reactance already resolved by the caller, same
	 *                                        rule
	 */
	public static VoltageDropInput toInput(VoltageDropRequest request, double resolvedResistanceOhmsPerMeter, double resolvedReactanceOhmsPerMeter) {
		return new VoltageDropInput(
				request.getCircuitType(),
				request.getCurrentAmps(),
				request.getOneWayLengthMeters(),
				resolvedResistanceOhmsPerMeter,
				resolvedReactanceOhmsPerMeter,
				request.getPowerFactor(),
				request.getSystemVoltage(),
				request.getParallelSetsPerPhase());
	}

	public static VoltageDropResponse toResponse(VoltageDropResult result) {
		return VoltageDropResponse.from(result);
	}

}
