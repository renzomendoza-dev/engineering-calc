package com.renzoproject.calc_api.mechanical.firepump;

import com.renzoproject.calc.core.mechanical.firepump.CandidatePumpCurve;
import com.renzoproject.calc.core.mechanical.firepump.FirePumpCurveValidationInput;
import com.renzoproject.calc.core.mechanical.firepump.FirePumpCurveValidationResult;
import com.renzoproject.calc.core.mechanical.firepump.FirePumpUnits;
import com.renzoproject.calc.core.mechanical.firepump.PumpCurvePoint;
import tech.units.indriya.quantity.Quantities;

public final class FirePumpCurveValidationMapper {

	private FirePumpCurveValidationMapper() {
	}

	public static FirePumpCurveValidationInput toCoreInput(FirePumpCurveValidationRequest request) {
		CandidatePumpCurve candidate = new CandidatePumpCurve(
				Quantities.getQuantity(request.candidateRatedFlowGpm(), FirePumpUnits.GPM),
				Quantities.getQuantity(request.candidateRatedPressurePsi(), FirePumpUnits.PSI),
				request.curvePoints().stream().map(FirePumpCurveValidationMapper::toCorePoint).toList());

		return new FirePumpCurveValidationInput(
				candidate,
				Quantities.getQuantity(request.demandFlowGpm(), FirePumpUnits.GPM),
				Quantities.getQuantity(request.demandPressurePsi(), FirePumpUnits.PSI));
	}

	public static FirePumpCurveValidationResponse toResponse(FirePumpCurveValidationResult result) {
		return new FirePumpCurveValidationResponse(
				result.meetsCapacityDemand(),
				result.churnPressure().to(FirePumpUnits.PSI).getValue().doubleValue(),
				result.churnCompliant(),
				result.overloadPressure().to(FirePumpUnits.PSI).getValue().doubleValue(),
				result.overloadCompliant(),
				result.overallCompliant());
	}

	private static PumpCurvePoint toCorePoint(PumpCurvePointDto dto) {
		return new PumpCurvePoint(
				Quantities.getQuantity(dto.flowGpm(), FirePumpUnits.GPM),
				Quantities.getQuantity(dto.pressurePsi(), FirePumpUnits.PSI));
	}

}
