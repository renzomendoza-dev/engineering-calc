package com.renzoproject.calc_api.electrical.wiresizing;

import com.renzoproject.calc.core.electrical.reference.ConductorMaterial;
import com.renzoproject.calc.core.electrical.reference.ConduitMaterial;
import com.renzoproject.calc.core.electrical.reference.InsulationType;
import com.renzoproject.calc.core.electrical.voltagedrop.CircuitType;
import com.renzoproject.calc.core.electrical.wiresizing.VoltageDropCheckRequest;
import com.renzoproject.calc.core.electrical.wiresizing.WireSizingInput;
import com.renzoproject.calc.core.electrical.wiresizing.WireSizingResult;
import com.renzoproject.calc_api.common.EnumParsing;

/**
 * Maps between calc-api's wire sizing DTOs and calc-core's calculator types.
 *
 * <p>All enum-ish fields ({@code circuitType}, {@code conductorMaterial},
 * {@code conduitMaterial}, {@code insulationType}) are accepted as plain strings and mapped
 * explicitly -- see {@link EnumParsing} for why. {@code InsulationType} uses its own
 * {@code fromLabel}, since its labels contain hyphens that aren't valid enum constant names.
 */
public final class WireSizingMapper {

	private WireSizingMapper() {
	}

	public static WireSizingInput toInput(WireSizingRequest request) {
		InsulationType insulationType = InsulationType.fromLabel(request.insulationType());
		ConductorMaterial conductorMaterial = EnumParsing.parse(ConductorMaterial.class, request.conductorMaterial(), "conductor material");
		VoltageDropCheckRequest voltageDropCheck = request.voltageDropCheck() == null
				? null
				: toVoltageDropCheckRequest(request.voltageDropCheck());

		return new WireSizingInput(
				request.loadCurrentAmps(),
				request.isContinuousLoad(),
				request.ambientTempCelsius(),
				request.numberOfCurrentCarryingConductors(),
				request.numberOfParallelSets(),
				insulationType,
				conductorMaterial,
				request.terminationTempRatingCelsius(),
				voltageDropCheck);
	}

	/**
	 * Public because {@code MotorConductorSizingMapper} embeds the same optional voltage drop check
	 * and reuses this conversion rather than keeping its own copy.
	 */
	public static VoltageDropCheckRequest toVoltageDropCheckRequest(VoltageDropCheckRequestDto dto) {
		CircuitType circuitType = EnumParsing.parse(CircuitType.class, dto.circuitType(), "circuit type");
		ConduitMaterial conduitMaterial = EnumParsing.parse(ConduitMaterial.class, dto.conduitMaterial(), "conduit material");
		return new VoltageDropCheckRequest(
				circuitType,
				dto.oneWayLengthMeters(),
				dto.powerFactor(),
				dto.systemVoltage(),
				conduitMaterial,
				dto.parallelSetsPerPhase());
	}

	public static WireSizingResponse toResponse(WireSizingResult result) {
		return WireSizingResponse.from(result);
	}

}
