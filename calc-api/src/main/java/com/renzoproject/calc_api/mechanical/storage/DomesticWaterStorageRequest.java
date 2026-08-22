package com.renzoproject.calc_api.mechanical.storage;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * HTTP request body for a domestic water storage calculation -- one endpoint, one shape,
 * covering both calc-core {@code DemandBasis} modes, same discriminator pattern as
 * {@code PipeVelocityRequest}'s {@code mode} field / {@code FirePumpPowerRequest}'s
 * {@code inputType} field.
 *
 * <p>{@link #isDemandBasisFieldsValid()} enforces {@code demandBasis}-dependent required fields,
 * same {@code @AssertTrue} pattern as those two requests -- only presence is checked here
 * (matching {@code FirePumpPowerRequest.isInputTypeFieldsValid()}'s convention), not deeper
 * value constraints like positivity; those stay in calc-core's {@code DomesticWaterStorageInput}
 * compact constructor and surface as a 400 via the existing {@code CalculationException} ->
 * {@code GlobalExceptionHandler} path.
 *
 * <p>{@code occupancyType} is accepted as a plain string, not validated against
 * {@code lpcd-consumption.json}'s actual keys here -- an unknown value fails at calculation time
 * via {@code PerCapitaConsumptionResolver} -> {@code CalculationException} -> 400, same as an
 * out-of-range WSFU. This codebase does have a precedent for exposing resolver-backed valid-value
 * lists via a dedicated reference endpoint (see {@code PipeReferenceController},
 * {@code ConductorReferenceController}), but adding one here is a separate increment, not part of
 * this task's two-endpoint scope.
 */
public record DomesticWaterStorageRequest(
		@NotNull DemandBasisDto demandBasis,

		// --- OCCUPANT_LOAD ---
		Integer occupantCount,
		String occupancyType,

		// --- FIXTURE_UNIT ---
		Double totalFixtureUnits,
		SystemTypeDto systemType,

		// --- both modes ---
		@NotNull @Positive Double storageDurationHours,
		Double safetyMarginPercent) {

	@AssertTrue(message = "For OCCUPANT_LOAD: occupantCount and occupancyType are required. "
			+ "For FIXTURE_UNIT: totalFixtureUnits and systemType are required.")
	public boolean isDemandBasisFieldsValid() {
		if (demandBasis == null) {
			return true; // let @NotNull on demandBasis itself report this
		}
		return switch (demandBasis) {
			case OCCUPANT_LOAD -> occupantCount != null && occupancyType != null && !occupancyType.isBlank();
			case FIXTURE_UNIT -> totalFixtureUnits != null && systemType != null;
		};
	}

}
