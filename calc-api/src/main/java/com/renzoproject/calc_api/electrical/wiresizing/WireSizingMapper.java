package com.renzoproject.calc_api.electrical.wiresizing;

import com.renzoproject.calc.core.electrical.reference.ConductorMaterial;
import com.renzoproject.calc.core.electrical.reference.ConduitMaterial;
import com.renzoproject.calc.core.electrical.reference.InsulationType;
import com.renzoproject.calc.core.electrical.voltagedrop.CircuitType;
import com.renzoproject.calc.core.electrical.wiresizing.VoltageDropCheckRequest;
import com.renzoproject.calc.core.electrical.wiresizing.WireSizingInput;
import com.renzoproject.calc.core.electrical.wiresizing.WireSizingResult;
import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Maps between calc-api's wire sizing DTOs and calc-core's calculator types.
 *
 * <p>All enum-ish fields ({@code circuitType}, {@code conductorMaterial},
 * {@code conduitMaterial}, {@code insulationType}) are accepted as plain strings and mapped
 * explicitly here rather than binding Jackson directly to the enum types — the same
 * rationale already documented on conduit fill's {@code ConductorFillEntryDto}: a bad value
 * should surface as a clear "Unknown X: value" {@code CalculationException} → 400 via the
 * existing {@code GlobalExceptionHandler}, not a generic Jackson deserialization failure.
 * {@code InsulationType} already has {@code fromLabel} for this; {@code CircuitType} /
 * {@code ConductorMaterial} / {@code ConduitMaterial} don't need one in calc-core (their
 * labels are already plain, hyphen-free enum-identifier-safe names), so {@link #parseEnum}
 * does the same "clear error" job generically for those three here.
 */
public final class WireSizingMapper {

	private WireSizingMapper() {
	}

	public static WireSizingInput toInput(WireSizingRequest request) {
		InsulationType insulationType = InsulationType.fromLabel(request.insulationType());
		ConductorMaterial conductorMaterial = parseEnum(ConductorMaterial.class, request.conductorMaterial(), "conductor material");
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

	private static VoltageDropCheckRequest toVoltageDropCheckRequest(VoltageDropCheckRequestDto dto) {
		CircuitType circuitType = parseEnum(CircuitType.class, dto.circuitType(), "circuit type");
		ConduitMaterial conduitMaterial = parseEnum(ConduitMaterial.class, dto.conduitMaterial(), "conduit material");
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

	private static <E extends Enum<E>> E parseEnum(Class<E> enumType, String rawValue, String fieldLabel) {
		try {
			return Enum.valueOf(enumType, rawValue);
		} catch (IllegalArgumentException e) {
			throw new CalculationException("Unknown " + fieldLabel + ": " + rawValue);
		}
	}

}
