package com.renzoproject.calc.core.mechanical.firepump;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;
import tech.units.indriya.quantity.Quantities;

import javax.measure.Quantity;
import javax.measure.quantity.Pressure;

/**
 * Validates a candidate pump curve against NFPA 20's shape rule: capacity at rated point, a
 * churn (shutoff, 0 GPM) pressure ceiling, and an overload (loaded flow %, typically 150% of
 * rated) pressure floor. The 140%/150%/65% figures are loaded via
 * {@link FirePumpCurveRequirementsLoader}, never hardcoded here.
 */
public class FirePumpCurveValidationCalculator implements Calculator<FirePumpCurveValidationInput, FirePumpCurveValidationResult> {

	private final FirePumpCurveRequirementsLoader requirementsLoader;

	public FirePumpCurveValidationCalculator(FirePumpCurveRequirementsLoader requirementsLoader) {
		this.requirementsLoader = requirementsLoader;
	}

	@Override
	public FirePumpCurveValidationResult calculate(FirePumpCurveValidationInput input) {
		FirePumpCurveRequirements requirements = requirementsLoader.load();
		CandidatePumpCurve candidate = input.candidate();

		double ratedFlowGpm = candidate.ratedFlow().to(FirePumpUnits.GPM).getValue().doubleValue();
		double ratedPressurePsi = candidate.ratedPressure().to(FirePumpUnits.PSI).getValue().doubleValue();
		double demandFlowGpm = input.demandFlow().to(FirePumpUnits.GPM).getValue().doubleValue();
		double demandPressurePsi = input.demandPressure().to(FirePumpUnits.PSI).getValue().doubleValue();

		boolean meetsCapacityDemand = ratedFlowGpm >= demandFlowGpm && ratedPressurePsi >= demandPressurePsi;

		Quantity<Pressure> churnPressure = PumpCurveInterpolator.interpolatePressureAt(
				candidate.curvePoints(), Quantities.getQuantity(0.0, FirePumpUnits.GPM));
		double churnPressurePsi = churnPressure.to(FirePumpUnits.PSI).getValue().doubleValue();
		double churnMaxPsi = ratedPressurePsi * (requirements.churnMaxPercentOfRated() / 100.0);
		boolean churnCompliant = churnPressurePsi <= churnMaxPsi;

		double overloadFlowGpm = ratedFlowGpm * (requirements.overloadFlowPercentOfRated() / 100.0);
		Quantity<Pressure> overloadPressure = PumpCurveInterpolator.interpolatePressureAt(
				candidate.curvePoints(), Quantities.getQuantity(overloadFlowGpm, FirePumpUnits.GPM));
		double overloadPressurePsi = overloadPressure.to(FirePumpUnits.PSI).getValue().doubleValue();
		double overloadMinPsi = ratedPressurePsi * (requirements.overloadMinPressurePercentOfRated() / 100.0);
		boolean overloadCompliant = overloadPressurePsi >= overloadMinPsi;

		boolean overallCompliant = meetsCapacityDemand && churnCompliant && overloadCompliant;

		return new FirePumpCurveValidationResult(
				meetsCapacityDemand, churnPressure, churnCompliant, overloadPressure, overloadCompliant, overallCompliant);
	}

}
