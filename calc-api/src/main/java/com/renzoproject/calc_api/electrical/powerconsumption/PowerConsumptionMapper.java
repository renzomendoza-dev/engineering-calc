package com.renzoproject.calc_api.electrical.powerconsumption;

import com.renzoproject.calc.core.electrical.powerconsumption.PowerConsumptionInput;
import com.renzoproject.calc.core.electrical.powerconsumption.PowerConsumptionResult;
import com.renzoproject.calc.core.electrical.powerconsumption.PowerInputMode;
import com.renzoproject.calc.core.electrical.voltagedrop.CircuitType;
import com.renzoproject.calc_api.common.EnumParsing;

/**
 * Maps between calc-api's power consumption DTOs and calc-core's calculator types.
 *
 * <p>{@code mode} is always mapped; {@code circuitType} is mapped only when present (a
 * {@code null} string stays {@code null} — {@code VOLTAGE_CURRENT} is the only mode that needs
 * it, and calc-core's own compact constructor is what actually enforces that). Every other field
 * passes straight through — same nullable {@code Double}/{@code double} shape calc-core already
 * expects.
 */
public final class PowerConsumptionMapper {

	private PowerConsumptionMapper() {
	}

	public static PowerConsumptionInput toInput(PowerConsumptionRequest request) {
		PowerInputMode mode = EnumParsing.parse(PowerInputMode.class, request.mode(), "power input mode");
		CircuitType circuitType = request.circuitType() == null
				? null
				: EnumParsing.parse(CircuitType.class, request.circuitType(), "circuit type");

		return new PowerConsumptionInput(
				mode,
				request.wattageInput(),
				circuitType,
				request.voltage(),
				request.currentAmps(),
				request.powerFactor(),
				request.horsepowerInput(),
				request.motorEfficiencyPercent(),
				request.totalOperatingHours(),
				request.pricePerKwh());
	}

	public static PowerConsumptionResponse toResponse(PowerConsumptionResult result) {
		return PowerConsumptionResponse.from(result);
	}

}
