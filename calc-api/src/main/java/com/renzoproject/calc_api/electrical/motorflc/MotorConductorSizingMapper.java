package com.renzoproject.calc_api.electrical.motorflc;

import com.renzoproject.calc.core.electrical.motorflc.MotorConductorSizingInput;
import com.renzoproject.calc.core.electrical.motorflc.MotorConductorSizingResult;
import com.renzoproject.calc.core.electrical.reference.ConductorMaterial;
import com.renzoproject.calc.core.electrical.reference.ConduitMaterial;
import com.renzoproject.calc.core.electrical.reference.InsulationType;
import com.renzoproject.calc.core.electrical.reference.MotorClass;
import com.renzoproject.calc.core.electrical.reference.MotorPhaseType;
import com.renzoproject.calc.core.electrical.voltagedrop.CircuitType;
import com.renzoproject.calc.core.electrical.wiresizing.VoltageDropCheckRequest;
import com.renzoproject.calc_api.electrical.wiresizing.VoltageDropCheckRequestDto;

/**
 * Maps between calc-api's motor conductor sizing DTOs and calc-core's calculator types.
 *
 * <p>Reuses {@link MotorFlcMapper#parseEnum} (package-private) for {@code phaseType} /
 * {@code motorClass} rather than a third copy of that helper. The
 * {@code VoltageDropCheckRequestDto -> VoltageDropCheckRequest} conversion mirrors
 * {@code WireSizingMapper.toVoltageDropCheckRequest} exactly, but is reimplemented locally here
 * (with its own {@code parseEnum} calls) rather than reusing that method directly, since it's
 * {@code private} to {@code WireSizingMapper} — widening its visibility would itself be a
 * modification to the wiresizing package, which this feature is scoped to avoid. Only the DTO
 * *types* ({@link VoltageDropCheckRequestDto}, and {@code WireSizingResponse} via
 * {@link MotorConductorSizingResponse}) are reused; the wiresizing package's own classes are
 * untouched.
 */
public final class MotorConductorSizingMapper {

	private MotorConductorSizingMapper() {
	}

	public static MotorConductorSizingInput toInput(MotorConductorSizingRequest request) {
		MotorPhaseType phaseType = MotorFlcMapper.parseEnum(MotorPhaseType.class, request.phaseType(), "motor phase type");
		MotorClass motorClass = request.motorClass() == null
				? null
				: MotorFlcMapper.parseEnum(MotorClass.class, request.motorClass(), "motor class");
		InsulationType insulationType = InsulationType.fromLabel(request.insulationType());
		ConductorMaterial conductorMaterial = MotorFlcMapper.parseEnum(ConductorMaterial.class, request.conductorMaterial(), "conductor material");
		VoltageDropCheckRequest voltageDropCheck = request.voltageDropCheck() == null
				? null
				: toVoltageDropCheckRequest(request.voltageDropCheck());

		return new MotorConductorSizingInput(
				phaseType,
				motorClass,
				request.horsepowerLabel(),
				request.voltage(),
				request.synchronousPowerFactorPercent(),
				request.ambientTempCelsius(),
				request.numberOfCurrentCarryingConductors(),
				request.numberOfParallelSets(),
				insulationType,
				conductorMaterial,
				request.terminationTempRatingCelsius(),
				voltageDropCheck);
	}

	private static VoltageDropCheckRequest toVoltageDropCheckRequest(VoltageDropCheckRequestDto dto) {
		CircuitType circuitType = MotorFlcMapper.parseEnum(CircuitType.class, dto.circuitType(), "circuit type");
		ConduitMaterial conduitMaterial = MotorFlcMapper.parseEnum(ConduitMaterial.class, dto.conduitMaterial(), "conduit material");
		return new VoltageDropCheckRequest(
				circuitType,
				dto.oneWayLengthMeters(),
				dto.powerFactor(),
				dto.systemVoltage(),
				conduitMaterial,
				dto.parallelSetsPerPhase());
	}

	public static MotorConductorSizingResponse toResponse(MotorConductorSizingResult result) {
		return MotorConductorSizingResponse.from(result);
	}

}
