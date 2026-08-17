package com.renzoproject.calc_api.smokecontrol;

import com.renzoproject.calc.core.smokecontrol.VentAreaInput;
import com.renzoproject.calc.core.smokecontrol.VentAreaResult;

/**
 * Pure mapping, no logic. {@code VentAreaResult} has no sealed/discriminated types -- no regime
 * branching like the plume calculators' results -- so unlike {@code SmokeProductionMapper}/
 * {@code TSquaredSmokeProductionMapper}, there's no switch expression here, just a
 * straightforward field-by-field conversion.
 */
public final class VentAreaMapper {

	private VentAreaMapper() {
	}

	public static VentAreaInput toCoreInput(VentAreaRequest request) {
		return new VentAreaInput(
				request.volumetricFlowRate(),
				request.smokeTemperature(),
				request.ambientTemperature(),
				request.ventHeight(),
				request.dischargeCoefficient());
	}

	public static VentAreaResponse toResponse(VentAreaResult result) {
		return new VentAreaResponse(result.deltaT(), result.requiredVentArea());
	}

}
