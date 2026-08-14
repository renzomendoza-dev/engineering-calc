package com.renzoproject.calc_api.mechanical.firepump;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * HTTP request body for a fire pump power/driver sizing calculation — one endpoint, one shape,
 * covering both calc-core {@code FirePumpPowerInput} variants, same discriminator pattern as
 * {@code PipeVelocityRequest}'s {@code mode} field.
 *
 * <p>There's no driver-type field here: this endpoint always sizes an electric motor. Diesel
 * driver sizing is explicitly out of scope for this task (calc-core has no clean standard data
 * for it — see {@code reference/firepump/README.md}), so {@link FirePumpPowerMapper} always
 * builds calc-core's electric variant.
 *
 * <p>{@link #isInputTypeFieldsValid()} enforces {@code inputType}-dependent required fields,
 * same {@code @AssertTrue} pattern as {@code PipeVelocityRequest.isModeFieldsValid()}.
 */
public record FirePumpPowerRequest(
		@NotNull FirePumpPowerInputTypeDto inputType,

		// --- RATED_POINT_ONLY ---
		Double flowGpm,
		Double pressurePsi,

		// --- FULL_CURVE ---
		Double curveRatedFlowGpm,
		Double curveRatedPressurePsi,
		List<@Valid PumpCurvePointDto> curvePoints,

		// --- both modes ---
		@NotNull @Positive Double pumpEfficiency,
		Double specificGravity) {

	@AssertTrue(message = "For RATED_POINT_ONLY: flowGpm and pressurePsi are required. "
			+ "For FULL_CURVE: curveRatedFlowGpm, curveRatedPressurePsi, and a non-empty "
			+ "curvePoints are required.")
	public boolean isInputTypeFieldsValid() {
		if (inputType == null) {
			return true; // let @NotNull on inputType itself report this
		}
		return switch (inputType) {
			case RATED_POINT_ONLY -> flowGpm != null && pressurePsi != null;
			case FULL_CURVE -> curveRatedFlowGpm != null && curveRatedPressurePsi != null
					&& curvePoints != null && !curvePoints.isEmpty();
		};
	}

}
