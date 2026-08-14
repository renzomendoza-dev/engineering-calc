package com.renzoproject.calc_api.mechanical.firepump;

import java.util.List;

public record FirePumpPowerResponse(
		List<PowerAtPointDto> evaluatedPoints,
		Double maxBrakeHorsepower,
		String recommendedElectricMotorSize,
		String driverSizingNote) {

}
