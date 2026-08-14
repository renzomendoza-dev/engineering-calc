package com.renzoproject.calc_api.mechanical.firepump;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * HTTP request body for a fire pump demand calculation. Units are fixed to psi/GPM throughout
 * this whole package (per calc-core's {@code mechanical.firepump} package-level convention) —
 * unlike the pipe velocity DTOs, there are no {@code *Unit} string fields here to parse.
 *
 * <p>{@link #isSuctionFieldsValid()} enforces {@code suctionCondition}-dependent required
 * fields, same {@code @AssertTrue} pattern as {@code PipeVelocityRequest.isModeFieldsValid()}.
 */
public record FirePumpDemandRequest(
		@NotNull @Positive Double requiredFlowGpm,
		@NotNull @Positive Double requiredPressurePsiAtDemandPoint,
		@NotNull SuctionConditionDto suctionCondition,
		Double availableSuctionPressurePsi,
		Double suctionLiftFeet,
		@PositiveOrZero Double safetyMarginPercent) {

	@AssertTrue(message = "When suctionCondition is FLOODED, availableSuctionPressurePsi is "
			+ "required and suctionLiftFeet must be omitted. When LIFT, suctionLiftFeet is "
			+ "required and availableSuctionPressurePsi must be omitted.")
	public boolean isSuctionFieldsValid() {
		if (suctionCondition == null) {
			return true; // let @NotNull on suctionCondition itself report this
		}
		return switch (suctionCondition) {
			case FLOODED -> availableSuctionPressurePsi != null && suctionLiftFeet == null;
			case LIFT -> suctionLiftFeet != null && availableSuctionPressurePsi == null;
		};
	}

}
