package com.renzoproject.calc.core.mechanical.firepump;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;

/**
 * Input for {@link FirePumpDemandCalculator}.
 *
 * @param requiredFlow                    hydraulically-calculated demand flow (from an external
 *                                         sprinkler/standpipe network calc — out of scope here);
 *                                         must be positive
 * @param requiredPressureAtDemandPoint   hydraulically-calculated demand pressure; must be
 *                                         positive
 * @param suctionCondition                which of {@code availableSuctionPressure}/
 *                                         {@code suctionLift} applies
 * @param availableSuctionPressure         required and non-null only when
 *                                         {@code suctionCondition} is {@link SuctionCondition#FLOODED}
 * @param suctionLift                      required and non-null only when
 *                                         {@code suctionCondition} is {@link SuctionCondition#LIFT}
 * @param safetyMarginPercent              applied to {@code requiredFlow}; {@code 0} means no
 *                                         margin (there's no such thing as an omitted record
 *                                         component, so callers who don't want a margin pass
 *                                         {@code 0.0} explicitly); must not be negative
 * @throws CalculationException if any validation rule above is violated
 */
public record FirePumpDemandInput(
		Quantity<VolumetricFlowRate> requiredFlow,
		Quantity<Pressure> requiredPressureAtDemandPoint,
		SuctionCondition suctionCondition,
		Quantity<Pressure> availableSuctionPressure,
		Quantity<Length> suctionLift,
		double safetyMarginPercent) {

	public FirePumpDemandInput {
		if (requiredFlow == null) {
			throw new CalculationException("requiredFlow is required");
		}
		if (requiredFlow.getValue().doubleValue() <= 0) {
			throw new CalculationException("requiredFlow must be positive");
		}
		if (requiredPressureAtDemandPoint == null) {
			throw new CalculationException("requiredPressureAtDemandPoint is required");
		}
		if (requiredPressureAtDemandPoint.getValue().doubleValue() <= 0) {
			throw new CalculationException("requiredPressureAtDemandPoint must be positive");
		}
		if (suctionCondition == null) {
			throw new CalculationException("suctionCondition is required");
		}
		if (safetyMarginPercent < 0) {
			throw new CalculationException("safetyMarginPercent must not be negative");
		}

		if (suctionCondition == SuctionCondition.FLOODED) {
			if (availableSuctionPressure == null) {
				throw new CalculationException("availableSuctionPressure is required when suctionCondition is FLOODED");
			}
			if (suctionLift != null) {
				throw new CalculationException("suctionLift must be null when suctionCondition is FLOODED");
			}
		} else {
			if (suctionLift == null) {
				throw new CalculationException("suctionLift is required when suctionCondition is LIFT");
			}
			if (availableSuctionPressure != null) {
				throw new CalculationException("availableSuctionPressure must be null when suctionCondition is LIFT");
			}
		}
	}

}
