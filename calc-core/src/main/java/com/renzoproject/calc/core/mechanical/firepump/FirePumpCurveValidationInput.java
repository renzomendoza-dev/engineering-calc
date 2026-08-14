package com.renzoproject.calc.core.mechanical.firepump;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;

import javax.measure.Quantity;
import javax.measure.quantity.Pressure;

public record FirePumpCurveValidationInput(
		CandidatePumpCurve candidate,
		Quantity<VolumetricFlowRate> demandFlow,
		Quantity<Pressure> demandPressure) {

	public FirePumpCurveValidationInput {
		if (candidate == null) {
			throw new CalculationException("candidate is required");
		}
		if (demandFlow == null) {
			throw new CalculationException("demandFlow is required");
		}
		if (demandPressure == null) {
			throw new CalculationException("demandPressure is required");
		}
	}

}
