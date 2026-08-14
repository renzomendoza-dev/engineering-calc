package com.renzoproject.calc_api.mechanical.firepump;

import com.renzoproject.calc.core.mechanical.firepump.CandidatePumpCurve;
import com.renzoproject.calc.core.mechanical.firepump.DriverType;
import com.renzoproject.calc.core.mechanical.firepump.FirePumpPowerInput;
import com.renzoproject.calc.core.mechanical.firepump.FirePumpPowerResult;
import com.renzoproject.calc.core.mechanical.firepump.FirePumpUnits;
import com.renzoproject.calc.core.mechanical.firepump.FullCurve;
import com.renzoproject.calc.core.mechanical.firepump.PowerAtPoint;
import com.renzoproject.calc.core.mechanical.firepump.PumpCurvePoint;
import com.renzoproject.calc.core.mechanical.firepump.RatedPointOnly;
import tech.units.indriya.quantity.Quantities;

public final class FirePumpPowerMapper {

	private FirePumpPowerMapper() {
	}

	public static FirePumpPowerInput toCoreInput(FirePumpPowerRequest request) {
		double pumpEfficiency = request.pumpEfficiency();
		double specificGravity = request.specificGravity() == null ? 1.0 : request.specificGravity();

		return switch (request.inputType()) {
			case RATED_POINT_ONLY -> new RatedPointOnly(
					Quantities.getQuantity(request.flowGpm(), FirePumpUnits.GPM),
					Quantities.getQuantity(request.pressurePsi(), FirePumpUnits.PSI),
					pumpEfficiency, specificGravity, DriverType.ELECTRIC);
			case FULL_CURVE -> new FullCurve(
					new CandidatePumpCurve(
							Quantities.getQuantity(request.curveRatedFlowGpm(), FirePumpUnits.GPM),
							Quantities.getQuantity(request.curveRatedPressurePsi(), FirePumpUnits.PSI),
							request.curvePoints().stream().map(FirePumpPowerMapper::toCorePoint).toList()),
					pumpEfficiency, specificGravity, DriverType.ELECTRIC);
		};
	}

	public static FirePumpPowerResponse toResponse(FirePumpPowerResult result) {
		var points = result.evaluatedPoints().stream().map(FirePumpPowerMapper::toPointDto).toList();
		return new FirePumpPowerResponse(points, result.maxBrakeHorsepower(), result.recommendedElectricMotorSize(), result.driverSizingNote());
	}

	private static PumpCurvePoint toCorePoint(PumpCurvePointDto dto) {
		return new PumpCurvePoint(
				Quantities.getQuantity(dto.flowGpm(), FirePumpUnits.GPM),
				Quantities.getQuantity(dto.pressurePsi(), FirePumpUnits.PSI));
	}

	private static PowerAtPointDto toPointDto(PowerAtPoint point) {
		return new PowerAtPointDto(
				point.flow().to(FirePumpUnits.GPM).getValue().doubleValue(),
				point.pressure().to(FirePumpUnits.PSI).getValue().doubleValue(),
				point.waterHorsepower(),
				point.brakeHorsepower());
	}

}
