package com.renzoproject.calc_api.mechanical.tank;

public record ExpansionTankResponse(
		Double derivedSystemVolumeLiters,
		Double expansionVolumeLiters,
		Double requiredTankVolumeLiters,
		Double coldWaterDensityKgM3,
		Double hotWaterDensityKgM3,
		Double fillPressureAbsoluteKpa,
		Double maxOperatingPressureAbsoluteKpa) {

}
