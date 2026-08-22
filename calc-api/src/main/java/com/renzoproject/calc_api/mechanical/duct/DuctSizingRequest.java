package com.renzoproject.calc_api.mechanical.duct;

import com.renzoproject.calc_api.mechanical.pipe.FrictionFactorMethodDto;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * HTTP request body for a duct sizing calculation. Deliberately mixed units matching HVAC
 * industry convention, not pure SI: millimeters for duct dimensions, L/s for airflow (matching
 * this app's SI-first convention for flow rate), m/s for velocity, Pa/m for friction rate,
 * Celsius/meters for temperature/altitude. Field names are explicit about this
 * ({@code equivalentDiameterMm}, not {@code equivalentDiameter}) since this endpoint's unit
 * choices differ from {@code PipePressureLossResponse}'s pure-SI (meters) convention -- see
 * {@link DuctSizingResponse}.
 *
 * <p>Reuses {@link FrictionFactorMethodDto} from {@code mechanical.pipe} rather than a duplicate
 * -- same enum, same two correlations, no reason for a second copy.
 *
 * <p>TWO independent conditional-required-field groups stacked here -- {@code shape} and
 * {@code method} -- each enforced by its own {@code @AssertTrue} method (only presence is
 * checked, not deeper constraints like positivity; those stay in calc-core's
 * {@code DuctSizingInput} compact constructor), same pattern as
 * {@code PipePressureLossRequest.isDiameterSpecFieldsValid()} /
 * {@code PipeVelocityRequest.isModeFieldsValid()}, applied twice rather than combined into one
 * check so a violation of either group reports its own clear message.
 */
public record DuctSizingRequest(
		@NotNull DuctSizingMethodDto method,
		@NotNull @Positive Double airFlowLps,
		@NotNull Double airTemperatureCelsius,
		@NotNull Double altitudeMeters,
		@NotNull String ductMaterial,
		@NotNull FrictionFactorMethodDto frictionMethod,

		// --- shape ---
		@NotNull DuctShapeDto shape,
		FixedDimensionTypeDto fixedDimensionType,
		Double fixedDimensionValueMm,

		// --- method ---
		Double targetFrictionRatePaPerM,
		Double maxVelocityMps) {

	@AssertTrue(message = "For RECTANGULAR shape: fixedDimensionType and fixedDimensionValueMm are required.")
	public boolean isShapeFieldsValid() {
		if (shape == null) {
			return true; // let @NotNull on shape itself report this
		}
		return shape != DuctShapeDto.RECTANGULAR || (fixedDimensionType != null && fixedDimensionValueMm != null);
	}

	@AssertTrue(message = "For EQUAL_FRICTION method: targetFrictionRatePaPerM is required. "
			+ "For VELOCITY method: maxVelocityMps is required.")
	public boolean isMethodFieldsValid() {
		if (method == null) {
			return true; // let @NotNull on method itself report this
		}
		return switch (method) {
			case EQUAL_FRICTION -> targetFrictionRatePaPerM != null;
			case VELOCITY -> maxVelocityMps != null;
		};
	}

}
