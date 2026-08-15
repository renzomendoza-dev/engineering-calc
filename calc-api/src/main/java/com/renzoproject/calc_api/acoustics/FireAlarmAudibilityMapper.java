package com.renzoproject.calc_api.acoustics;

import com.renzoproject.calc.core.acoustics.AudibilityOutcome;
import com.renzoproject.calc.core.acoustics.FireAlarmAudibilityInput;
import com.renzoproject.calc.core.acoustics.FireAlarmAudibilityResult;
import com.renzoproject.calc.core.acoustics.GoverningAudibilityRule;
import com.renzoproject.calc.core.acoustics.PrivateModeInput;
import com.renzoproject.calc.core.acoustics.PublicModeInput;
import com.renzoproject.calc.core.acoustics.SleepingAreaInput;

/**
 * {@code toCoreInput} is the one place with real branching -- a switch on {@code mode} choosing
 * which sealed-interface subtype to construct. Everything else (the enum mirrors in
 * {@code toResponse}) is pure mapping, no logic, consistent with every other mapper in this
 * codebase.
 */
public final class FireAlarmAudibilityMapper {

	private FireAlarmAudibilityMapper() {
	}

	public static FireAlarmAudibilityInput toCoreInput(FireAlarmAudibilityRequest request) {
		return switch (request.mode()) {
			case PUBLIC -> new PublicModeInput(
					request.applianceSplAtReferenceDb(),
					request.referenceDistanceMeters(),
					request.targetDistanceMeters(),
					request.measuredAverageAmbientDb(),
					request.measuredMaxSustainedAmbientDb());
			case PRIVATE -> new PrivateModeInput(
					request.applianceSplAtReferenceDb(),
					request.referenceDistanceMeters(),
					request.targetDistanceMeters(),
					request.measuredAverageAmbientDb(),
					request.measuredMaxSustainedAmbientDb());
			case SLEEPING -> new SleepingAreaInput(
					request.applianceSplAtReferenceDb(),
					request.referenceDistanceMeters(),
					request.targetDistanceMeters(),
					request.measuredAverageAmbientDb(),
					request.measuredMaxSustainedAmbientDb());
		};
	}

	public static FireAlarmAudibilityResponse toResponse(FireAlarmAudibilityResult result) {
		return new FireAlarmAudibilityResponse(
				result.calculatedTargetSplDb(),
				result.requiredThresholdDb(),
				toDtoRule(result.governingRule()),
				toDtoOutcome(result.outcome()));
	}

	private static GoverningAudibilityRuleDto toDtoRule(GoverningAudibilityRule rule) {
		return switch (rule) {
			case AVERAGE_AMBIENT_PLUS_OFFSET -> GoverningAudibilityRuleDto.AVERAGE_AMBIENT_PLUS_OFFSET;
			case MAX_SUSTAINED_PLUS_OFFSET -> GoverningAudibilityRuleDto.MAX_SUSTAINED_PLUS_OFFSET;
			case ABSOLUTE_SLEEPING_FLOOR -> GoverningAudibilityRuleDto.ABSOLUTE_SLEEPING_FLOOR;
		};
	}

	private static AudibilityOutcomeDto toDtoOutcome(AudibilityOutcome outcome) {
		return switch (outcome) {
			case PASS -> AudibilityOutcomeDto.PASS;
			case FAIL -> AudibilityOutcomeDto.FAIL;
			case EXCEEDS_MAX_LIMIT -> AudibilityOutcomeDto.EXCEEDS_MAX_LIMIT;
		};
	}

}
