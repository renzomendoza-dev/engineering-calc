package com.renzoproject.calc_api.mechanical.pipe;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * HTTP request body for a pipe velocity/sizing calculation. One endpoint, one shape, covering
 * both calc-core modes — {@code mode} discriminates which of the two groups of otherwise-optional
 * fields below actually apply, the same way {@code VoltageDropRequest}'s
 * {@code useCustomImpedance} flag switches between two field groups on one request shape.
 *
 * <p>Unlike {@code VoltageDropRequest}, calc-core has no existing Indriya unit-string handling
 * to check and mirror — voltage drop's calc-core input uses plain doubles, no units at all. The
 * {@code *Unit} string fields here, and their parsing, are new; see {@link PipeVelocityMapper}
 * for the supported unit strings.
 *
 * <p>{@link #isModeFieldsValid()} enforces which fields are required for the selected
 * {@code mode} (and, within {@code VELOCITY_FROM_DIAMETER}, the selected
 * {@code diameterSpecType}) — same {@code @AssertTrue} style as
 * {@code VoltageDropRequest.isImpedanceSelectionValid()}. {@code nominalSchedule}/{@code schedule}
 * are deliberately NOT enforced here even though some materials require them: whether a given
 * material needs a schedule is reference-data content only calc-core's
 * {@code PipeDimensionResolver} knows about, and it already throws a clear
 * {@code CalculationException} (→ 400 via the existing handler) when one is missing and needed.
 * Duplicating that judgment here would mean either hardcoding which materials need schedules or
 * always requiring it and breaking single-schedule materials — the same reasoning
 * {@code VoltageDropRequest} already applies to leaving its AC-only power-factor rule to
 * calc-core rather than a second validator.
 */
public record PipeVelocityRequest(
		@NotNull PipeSizingModeDto mode,
		@NotNull @Positive Double flowRateValue,
		String flowRateUnit,

		// --- diameter spec (VELOCITY_FROM_DIAMETER mode) ---
		DiameterSpecTypeDto diameterSpecType,
		String nominalMaterial,
		String nominalSchedule,
		String nominalLabel,
		Double rawDiameterValue,
		String rawDiameterUnit,

		// --- design mode (DIAMETER_FROM_VELOCITY mode) ---
		Double targetVelocityValue,
		String targetVelocityUnit,
		String pipeMaterial,
		String schedule) {

	@AssertTrue(message = "For VELOCITY_FROM_DIAMETER mode: diameterSpecType is required, plus "
			+ "nominalMaterial/nominalLabel (NOMINAL) or rawDiameterValue/rawDiameterUnit (RAW). "
			+ "For DIAMETER_FROM_VELOCITY mode: targetVelocityValue, targetVelocityUnit, and "
			+ "pipeMaterial are required.")
	public boolean isModeFieldsValid() {
		if (mode == null) {
			return true; // let @NotNull on mode itself report this
		}
		if (mode == PipeSizingModeDto.VELOCITY_FROM_DIAMETER) {
			if (diameterSpecType == null) {
				return false;
			}
			return switch (diameterSpecType) {
				case NOMINAL -> notBlank(nominalMaterial) && notBlank(nominalLabel);
				case RAW -> rawDiameterValue != null && notBlank(rawDiameterUnit);
			};
		}
		return targetVelocityValue != null && notBlank(targetVelocityUnit) && notBlank(pipeMaterial);
	}

	private static boolean notBlank(String value) {
		return value != null && !value.isBlank();
	}

}
