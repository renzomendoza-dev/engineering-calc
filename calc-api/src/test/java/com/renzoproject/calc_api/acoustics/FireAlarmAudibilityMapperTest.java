package com.renzoproject.calc_api.acoustics;

import com.renzoproject.calc.core.acoustics.AudibilityOutcome;
import com.renzoproject.calc.core.acoustics.FireAlarmAudibilityInput;
import com.renzoproject.calc.core.acoustics.FireAlarmAudibilityResult;
import com.renzoproject.calc.core.acoustics.GoverningAudibilityRule;
import com.renzoproject.calc.core.acoustics.PrivateModeInput;
import com.renzoproject.calc.core.acoustics.PublicModeInput;
import com.renzoproject.calc.core.acoustics.SleepingAreaInput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class FireAlarmAudibilityMapperTest {

	private static final double DELTA = 1e-9;

	@Test
	void toCoreInput_publicMode_buildsPublicModeInput() {
		FireAlarmAudibilityRequest request = new FireAlarmAudibilityRequest(FireAlarmNotificationMode.PUBLIC, 70.0, 1.0, 2.0, 50.0, 55.0);

		FireAlarmAudibilityInput input = FireAlarmAudibilityMapper.toCoreInput(request);

		PublicModeInput publicInput = assertInstanceOf(PublicModeInput.class, input);
		assertEquals(70.0, publicInput.applianceSplAtReferenceDb(), DELTA);
		assertEquals(1.0, publicInput.referenceDistanceMeters(), DELTA);
		assertEquals(2.0, publicInput.targetDistanceMeters(), DELTA);
		assertEquals(50.0, publicInput.measuredAverageAmbientDb(), DELTA);
		assertEquals(55.0, publicInput.measuredMaxSustainedAmbientDb(), DELTA);
	}

	@Test
	void toCoreInput_privateMode_buildsPrivateModeInput() {
		FireAlarmAudibilityRequest request = new FireAlarmAudibilityRequest(FireAlarmNotificationMode.PRIVATE, 60.0, 1.0, 1.0, 50.0, null);

		FireAlarmAudibilityInput input = FireAlarmAudibilityMapper.toCoreInput(request);

		assertInstanceOf(PrivateModeInput.class, input);
	}

	@Test
	void toCoreInput_sleepingMode_buildsSleepingAreaInput() {
		FireAlarmAudibilityRequest request = new FireAlarmAudibilityRequest(FireAlarmNotificationMode.SLEEPING, 80.0, 1.0, 1.0, 40.0, null);

		FireAlarmAudibilityInput input = FireAlarmAudibilityMapper.toCoreInput(request);

		assertInstanceOf(SleepingAreaInput.class, input);
	}

	@Test
	void toCoreInput_omittedMaxSustained_mapsToNull() {
		FireAlarmAudibilityRequest request = new FireAlarmAudibilityRequest(FireAlarmNotificationMode.PUBLIC, 70.0, 1.0, 1.0, 50.0, null);

		FireAlarmAudibilityInput input = FireAlarmAudibilityMapper.toCoreInput(request);

		assertEquals(null, input.measuredMaxSustainedAmbientDb());
	}

	@Test
	void toResponse_mapsAllFieldsAndMirrorsEnums() {
		FireAlarmAudibilityResult result = new FireAlarmAudibilityResult(70.0, 65.0, GoverningAudibilityRule.AVERAGE_AMBIENT_PLUS_OFFSET, AudibilityOutcome.PASS);

		FireAlarmAudibilityResponse response = FireAlarmAudibilityMapper.toResponse(result);

		assertEquals(70.0, response.calculatedTargetSplDb(), DELTA);
		assertEquals(65.0, response.requiredThresholdDb(), DELTA);
		assertEquals(GoverningAudibilityRuleDto.AVERAGE_AMBIENT_PLUS_OFFSET, response.governingRule());
		assertEquals(AudibilityOutcomeDto.PASS, response.outcome());
	}

	@Test
	void toResponse_mirrorsMaxSustainedRuleAndFailOutcome() {
		FireAlarmAudibilityResult result = new FireAlarmAudibilityResult(60.0, 75.0, GoverningAudibilityRule.MAX_SUSTAINED_PLUS_OFFSET, AudibilityOutcome.FAIL);

		FireAlarmAudibilityResponse response = FireAlarmAudibilityMapper.toResponse(result);

		assertEquals(GoverningAudibilityRuleDto.MAX_SUSTAINED_PLUS_OFFSET, response.governingRule());
		assertEquals(AudibilityOutcomeDto.FAIL, response.outcome());
	}

	@Test
	void toResponse_mirrorsSleepingFloorRuleAndExceedsMaxOutcome() {
		FireAlarmAudibilityResult result = new FireAlarmAudibilityResult(120.0, 75.0, GoverningAudibilityRule.ABSOLUTE_SLEEPING_FLOOR, AudibilityOutcome.EXCEEDS_MAX_LIMIT);

		FireAlarmAudibilityResponse response = FireAlarmAudibilityMapper.toResponse(result);

		assertEquals(GoverningAudibilityRuleDto.ABSOLUTE_SLEEPING_FLOOR, response.governingRule());
		assertEquals(AudibilityOutcomeDto.EXCEEDS_MAX_LIMIT, response.outcome());
	}

}
