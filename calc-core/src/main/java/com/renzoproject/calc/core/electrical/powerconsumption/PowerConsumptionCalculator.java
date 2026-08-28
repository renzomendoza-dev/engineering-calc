package com.renzoproject.calc.core.electrical.powerconsumption;

import com.renzoproject.calc.core.Calculator;

/**
 * Estimates electrical energy consumption (kWh) and optional cost for a piece of equipment,
 * given its power draw (specified one of three ways -- see {@link PowerInputMode}) and how long
 * it runs. Pure arithmetic -- no reference tables, no injected dependencies.
 */
public class PowerConsumptionCalculator implements Calculator<PowerConsumptionInput, PowerConsumptionResult> {

	private static final double DEFAULT_POWER_FACTOR = 1.0;
	private static final double DEFAULT_MOTOR_EFFICIENCY_PERCENT = 85.0;
	private static final double WATTS_PER_HORSEPOWER = 746.0;

	@Override
	public PowerConsumptionResult calculate(PowerConsumptionInput input) {
		double powerWatts = switch (input.mode()) {
			case DIRECT_WATTAGE -> input.wattageInput();
			case VOLTAGE_CURRENT -> powerWattsFromVoltageCurrent(input);
			case HORSEPOWER -> powerWattsFromHorsepower(input);
		};

		double powerKw = powerWatts / 1000.0;
		double energyConsumedKwh = powerKw * input.totalOperatingHours();
		Double estimatedCost = input.pricePerKwh() != null ? energyConsumedKwh * input.pricePerKwh() : null;
		boolean isEstimatedPower = input.mode() == PowerInputMode.HORSEPOWER;

		return new PowerConsumptionResult(powerWatts, powerKw, energyConsumedKwh, estimatedCost, isEstimatedPower);
	}

	private static double powerWattsFromVoltageCurrent(PowerConsumptionInput input) {
		double effectivePowerFactor = input.powerFactor() != null ? input.powerFactor() : DEFAULT_POWER_FACTOR;
		return switch (input.circuitType()) {
			case DC -> input.voltage() * input.currentAmps();
			case SINGLE_PHASE_AC -> input.voltage() * input.currentAmps() * effectivePowerFactor;
			case THREE_PHASE_AC -> Math.sqrt(3) * input.voltage() * input.currentAmps() * effectivePowerFactor;
		};
	}

	private static double powerWattsFromHorsepower(PowerConsumptionInput input) {
		double effectiveEfficiencyPercent = input.motorEfficiencyPercent() != null
				? input.motorEfficiencyPercent()
				: DEFAULT_MOTOR_EFFICIENCY_PERCENT;
		return (input.horsepowerInput() * WATTS_PER_HORSEPOWER) / (effectiveEfficiencyPercent / 100.0);
	}

}
