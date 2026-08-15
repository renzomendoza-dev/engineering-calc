package com.renzoproject.calc.core.acoustics;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Determines whether a fire alarm notification appliance meets NFPA 72 audibility requirements
 * at a given location: projects the appliance's rated SPL to the target distance via
 * {@link AcousticsMath#distanceAttenuationDb}, resolves the applicable threshold rule from
 * {@link AudibilityThresholdResolver}, and compares.
 *
 * <p><b>Sleeping-area rule choice:</b> the reference JSON's {@code sleepingArea} section is an
 * absolute floor only ({@code minimumDbaAtPillow}) that "can still be overridden upward by the
 * relative public/private rule if the space's ambient is high enough" -- but doesn't specify
 * which of the two relative rules applies. This calculator uses {@link AudibilityThresholdResolver#publicMode()}
 * for that override check, since NFPA 72's general audibility rule (the one the JSON calls
 * "public") is the one that governs occupant-notification spaces generally, including sleeping
 * areas; {@code privateMode()} is the narrower staff-alert-only exception and isn't the natural
 * default here. Flagged in case the source data intends otherwise.
 */
public class FireAlarmAudibilityCalculator implements Calculator<FireAlarmAudibilityInput, FireAlarmAudibilityResult> {

	private static final String GREATER_OF_RULE = "greaterOf";
	private static final String SLEEPING_FLOOR_RULE = "absoluteFloor_overriddenByRelativeRuleIfGreater";

	private final AudibilityThresholdResolver thresholdResolver;

	public FireAlarmAudibilityCalculator(AudibilityThresholdResolver thresholdResolver) {
		this.thresholdResolver = thresholdResolver;
	}

	@Override
	public FireAlarmAudibilityResult calculate(FireAlarmAudibilityInput input) {
		double attenuationDb = AcousticsMath.distanceAttenuationDb(input.referenceDistanceMeters(), input.targetDistanceMeters());
		double targetSplDb = input.applianceSplAtReferenceDb() + attenuationDb;

		ThresholdEvaluation winner = switch (input) {
			case PublicModeInput pub -> greaterOfCandidate(thresholdResolver.publicMode(), pub.measuredAverageAmbientDb(), pub.measuredMaxSustainedAmbientDb());
			case PrivateModeInput priv -> greaterOfCandidate(thresholdResolver.privateMode(), priv.measuredAverageAmbientDb(), priv.measuredMaxSustainedAmbientDb());
			case SleepingAreaInput sleep -> sleepingAreaCandidate(sleep);
		};

		double maximumAllowedDba = thresholdResolver.systemWideLimits().maximumAllowedDba();
		AudibilityOutcome outcome;
		if (targetSplDb > maximumAllowedDba) {
			outcome = AudibilityOutcome.EXCEEDS_MAX_LIMIT;
		} else if (targetSplDb >= winner.thresholdDb()) {
			outcome = AudibilityOutcome.PASS;
		} else {
			outcome = AudibilityOutcome.FAIL;
		}

		return new FireAlarmAudibilityResult(targetSplDb, winner.thresholdDb(), winner.rule(), outcome);
	}

	private ThresholdEvaluation greaterOfCandidate(NotificationModeThreshold rule, double measuredAverageAmbientDb, Double measuredMaxSustainedAmbientDb) {
		if (!GREATER_OF_RULE.equals(rule.rule())) {
			throw new CalculationException("Unsupported notification-mode threshold rule: " + rule.rule());
		}

		double averageCandidate = measuredAverageAmbientDb + rule.aboveAverageAmbientDb();
		if (measuredMaxSustainedAmbientDb == null) {
			return new ThresholdEvaluation(averageCandidate, GoverningAudibilityRule.AVERAGE_AMBIENT_PLUS_OFFSET);
		}

		double maxSustainedCandidate = measuredMaxSustainedAmbientDb + rule.aboveMaxSustainedDb();
		return maxSustainedCandidate > averageCandidate
				? new ThresholdEvaluation(maxSustainedCandidate, GoverningAudibilityRule.MAX_SUSTAINED_PLUS_OFFSET)
				: new ThresholdEvaluation(averageCandidate, GoverningAudibilityRule.AVERAGE_AMBIENT_PLUS_OFFSET);
	}

	private ThresholdEvaluation sleepingAreaCandidate(SleepingAreaInput input) {
		SleepingAreaThreshold floor = thresholdResolver.sleepingArea();
		if (!SLEEPING_FLOOR_RULE.equals(floor.rule())) {
			throw new CalculationException("Unsupported sleeping-area threshold rule: " + floor.rule());
		}

		ThresholdEvaluation relative = greaterOfCandidate(thresholdResolver.publicMode(), input.measuredAverageAmbientDb(), input.measuredMaxSustainedAmbientDb());
		return relative.thresholdDb() > floor.minimumDbaAtPillow()
				? relative
				: new ThresholdEvaluation(floor.minimumDbaAtPillow(), GoverningAudibilityRule.ABSOLUTE_SLEEPING_FLOOR);
	}

	private record ThresholdEvaluation(double thresholdDb, GoverningAudibilityRule rule) {

	}

}
