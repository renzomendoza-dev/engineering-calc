package com.renzoproject.calc_api.mechanical.tank;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * HTTP request body for an expansion tank sizing calculation. SI throughout, HVAC-industry mixed
 * units at the boundary (liters for volume, mm for pipe diameter, meters for length/altitude,
 * Celsius, kPa for pressure) -- same "SI internally, industry units at the DTO boundary"
 * convention as {@code DuctSizingRequest}.
 *
 * <p>{@code additionalVolumeFactor} is nullable here even though calc-core's
 * {@code ExpansionTankInput} requires a positive value -- {@link ExpansionTankMapper} defaults a
 * missing value to {@code 1.0} before constructing calc-core's input, an API-layer convenience
 * that shouldn't be pushed down into calc-core itself.
 */
public record ExpansionTankRequest(
		@NotNull SystemTypeDto systemType,
		@NotNull @Positive Double primaryVesselVolumeLiters,
		@NotNull @PositiveOrZero Double estimatedPipingLengthMeters,
		@NotNull @Positive Double averagePipeDiameterMm,
		Double additionalVolumeFactor,
		@NotNull Double coldWaterTemperatureCelsius,
		@NotNull Double hotWaterTemperatureCelsius,
		@NotNull @Positive Double fillPressureGaugeKpa,
		@NotNull @Positive Double maxOperatingPressureGaugeKpa,
		@NotNull Double altitudeMeters) {

}
