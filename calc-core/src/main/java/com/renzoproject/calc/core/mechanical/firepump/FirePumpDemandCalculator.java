package com.renzoproject.calc.core.mechanical.firepump;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;
import tech.units.indriya.quantity.Quantities;

/**
 * Converts a hydraulically-calculated demand (flow + pressure at the demand point) into the
 * flow/pressure the pump itself must be rated to deliver, accounting for suction conditions and
 * an optional safety margin.
 *
 * <p>Pure computation — no reference-data lookups, unlike the other three calculators in this
 * package.
 */
public class FirePumpDemandCalculator implements Calculator<FirePumpDemandInput, FirePumpDemandResult> {

	/** Rule-of-thumb conversion: 1 ft of water column = 0.433 psi. */
	private static final double PSI_PER_FOOT_OF_LIFT = 0.433;

	@Override
	public FirePumpDemandResult calculate(FirePumpDemandInput input) {
		double requiredFlowGpm = input.requiredFlow().to(FirePumpUnits.GPM).getValue().doubleValue();
		double ratedFlowGpm = requiredFlowGpm * (1 + input.safetyMarginPercent() / 100.0);

		double requiredPressurePsi = input.requiredPressureAtDemandPoint().to(FirePumpUnits.PSI).getValue().doubleValue();
		double ratedPressurePsi;
		if (input.suctionCondition() == SuctionCondition.FLOODED) {
			double availableSuctionPsi = input.availableSuctionPressure().to(FirePumpUnits.PSI).getValue().doubleValue();
			ratedPressurePsi = requiredPressurePsi - availableSuctionPsi;
		} else {
			double liftFeet = input.suctionLift().to(FirePumpUnits.FOOT).getValue().doubleValue();
			double liftPressureEquivalentPsi = liftFeet * PSI_PER_FOOT_OF_LIFT;
			ratedPressurePsi = requiredPressurePsi + liftPressureEquivalentPsi;
		}

		if (ratedPressurePsi <= 0) {
			throw new CalculationException("Computed ratedPressure is not positive (" + ratedPressurePsi
					+ " psi) -- check requiredPressureAtDemandPoint, availableSuctionPressure, and suctionLift inputs");
		}

		return new FirePumpDemandResult(
				Quantities.getQuantity(ratedFlowGpm, FirePumpUnits.GPM),
				Quantities.getQuantity(ratedPressurePsi, FirePumpUnits.PSI));
	}

}
