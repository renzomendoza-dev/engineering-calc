package com.renzoproject.calc_api.mechanical.tank;

public record PressureTankResponse(
		Double drawdownVolumeLiters,
		Double requiredTankVolumeLiters,
		Double lowPressureAbsoluteKpa,
		Double highPressureAbsoluteKpa) {

}
