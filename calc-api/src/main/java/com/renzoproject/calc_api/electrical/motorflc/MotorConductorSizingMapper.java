package com.renzoproject.calc_api.electrical.motorflc;

import com.renzoproject.calc.core.electrical.motorflc.MotorConductorSizingInput;
import com.renzoproject.calc.core.electrical.motorflc.MotorConductorSizingResult;
import com.renzoproject.calc.core.electrical.reference.ConductorMaterial;
import com.renzoproject.calc.core.electrical.reference.InsulationType;
import com.renzoproject.calc.core.electrical.reference.MotorClass;
import com.renzoproject.calc.core.electrical.reference.MotorPhaseType;
import com.renzoproject.calc.core.electrical.wiresizing.VoltageDropCheckRequest;
import com.renzoproject.calc_api.common.EnumParsing;
import com.renzoproject.calc_api.electrical.wiresizing.WireSizingMapper;

/**
 * Maps between calc-api's motor conductor sizing DTOs and calc-core's calculator types.
 *
 * <p>The optional voltage drop check is converted by {@link WireSizingMapper#toVoltageDropCheckRequest},
 * since this endpoint embeds exactly the same check as wire sizing.
 */
public final class MotorConductorSizingMapper {

	private MotorConductorSizingMapper() {
	}

	public static MotorConductorSizingInput toInput(MotorConductorSizingRequest request) {
		MotorPhaseType phaseType = EnumParsing.parse(MotorPhaseType.class, request.phaseType(), "motor phase type");
		MotorClass motorClass = request.motorClass() == null
				? null
				: EnumParsing.parse(MotorClass.class, request.motorClass(), "motor class");
		InsulationType insulationType = InsulationType.fromLabel(request.insulationType());
		ConductorMaterial conductorMaterial = EnumParsing.parse(ConductorMaterial.class, request.conductorMaterial(), "conductor material");
		VoltageDropCheckRequest voltageDropCheck = request.voltageDropCheck() == null
				? null
				: WireSizingMapper.toVoltageDropCheckRequest(request.voltageDropCheck());

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

	public static MotorConductorSizingResponse toResponse(MotorConductorSizingResult result) {
		return MotorConductorSizingResponse.from(result);
	}

}
