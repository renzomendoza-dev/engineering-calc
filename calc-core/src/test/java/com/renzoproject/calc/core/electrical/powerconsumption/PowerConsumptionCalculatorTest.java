package com.renzoproject.calc.core.electrical.powerconsumption;

import com.renzoproject.calc.core.electrical.voltagedrop.CircuitType;
import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerConsumptionCalculatorTest {

	private static final double DELTA = 1e-9;

	private final PowerConsumptionCalculator calculator = new PowerConsumptionCalculator();

	private static PowerConsumptionInput directWattage(double wattageInput, double totalOperatingHours, Double pricePerKwh) {
		return new PowerConsumptionInput(
				PowerInputMode.DIRECT_WATTAGE, wattageInput, null, null, null, null, null, null,
				totalOperatingHours, pricePerKwh);
	}

	private static PowerConsumptionInput voltageCurrent(
			CircuitType circuitType, double voltage, double currentAmps, Double powerFactor, double totalOperatingHours) {
		return new PowerConsumptionInput(
				PowerInputMode.VOLTAGE_CURRENT, null, circuitType, voltage, currentAmps, powerFactor, null, null,
				totalOperatingHours, null);
	}

	private static PowerConsumptionInput horsepower(
			double horsepowerInput, Double motorEfficiencyPercent, double totalOperatingHours) {
		return new PowerConsumptionInput(
				PowerInputMode.HORSEPOWER, null, null, null, null, null, horsepowerInput, motorEfficiencyPercent,
				totalOperatingHours, null);
	}

	@Test
	void directWattage_matchesHandCalculatedValue() {
		// 65W charger, 5 hours -> 65/1000 * 5 = 0.325 kWh
		PowerConsumptionResult result = calculator.calculate(directWattage(65.0, 5.0, null));

		assertEquals(65.0, result.powerWatts(), DELTA);
		assertEquals(0.065, result.powerKw(), DELTA);
		assertEquals(0.325, result.energyConsumedKwh(), DELTA);
		assertFalse(result.isEstimatedPower());
	}

	@Test
	void voltageCurrent_dc_noPowerFactorApplied() {
		// P = V * I = 48 * 10 = 480W, no PF term
		PowerConsumptionResult result = calculator.calculate(voltageCurrent(CircuitType.DC, 48.0, 10.0, null, 2.0));

		assertEquals(480.0, result.powerWatts(), DELTA);
		assertFalse(result.isEstimatedPower());
	}

	@Test
	void voltageCurrent_singlePhaseAc_explicitPowerFactor() {
		// P = V * I * PF = 230 * 10 * 0.9 = 2070W
		PowerConsumptionResult result = calculator.calculate(voltageCurrent(CircuitType.SINGLE_PHASE_AC, 230.0, 10.0, 0.9, 2.0));

		assertEquals(2070.0, result.powerWatts(), DELTA);
	}

	@Test
	void voltageCurrent_singlePhaseAc_noPowerFactor_defaultsToOne() {
		// PF omitted -> defaults to 1.0, P = V * I = 230 * 10 = 2300W
		PowerConsumptionResult result = calculator.calculate(voltageCurrent(CircuitType.SINGLE_PHASE_AC, 230.0, 10.0, null, 2.0));

		assertEquals(2300.0, result.powerWatts(), DELTA);
	}

	@Test
	void voltageCurrent_threePhaseAc() {
		// P = sqrt(3) * V * I * PF = sqrt(3) * 400 * 20 * 0.85
		PowerConsumptionResult result = calculator.calculate(voltageCurrent(CircuitType.THREE_PHASE_AC, 400.0, 20.0, 0.85, 2.0));

		double expected = Math.sqrt(3) * 400.0 * 20.0 * 0.85;
		assertEquals(expected, result.powerWatts(), DELTA);
	}

	@Test
	void voltageCurrent_dc_withPowerFactor_throws() {
		assertThrows(CalculationException.class, () -> voltageCurrent(CircuitType.DC, 48.0, 10.0, 1.0, 2.0));
	}

	@Test
	void horsepower_explicitEfficiency_isEstimatedPower() {
		// P = (hp * 746) / (efficiency/100) = (2 * 746) / (0.90) = 1657.777...
		PowerConsumptionResult result = calculator.calculate(horsepower(2.0, 90.0, 3.0));

		double expected = (2.0 * 746.0) / (90.0 / 100.0);
		assertEquals(expected, result.powerWatts(), DELTA);
		assertTrue(result.isEstimatedPower());
	}

	@Test
	void horsepower_noEfficiency_defaultsTo85Percent() {
		// efficiency omitted -> defaults to 85.0, P = (1 * 746) / 0.85
		PowerConsumptionResult result = calculator.calculate(horsepower(1.0, null, 3.0));

		double expected = (1.0 * 746.0) / (85.0 / 100.0);
		assertEquals(expected, result.powerWatts(), DELTA);
	}

	@Test
	void pricePerKwhProvided_estimatedCostComputed() {
		PowerConsumptionResult result = calculator.calculate(directWattage(1000.0, 10.0, 0.15));

		// energyConsumedKwh = 1.0 * 10 = 10 kWh, cost = 10 * 0.15 = 1.5
		assertEquals(10.0, result.energyConsumedKwh(), DELTA);
		assertEquals(1.5, result.estimatedCost(), DELTA);
	}

	@Test
	void pricePerKwhNotProvided_estimatedCostIsNull() {
		PowerConsumptionResult result = calculator.calculate(directWattage(1000.0, 10.0, null));

		assertNull(result.estimatedCost());
	}

	@Test
	void directWattage_withVoltageAndCurrent_throws() {
		assertThrows(CalculationException.class, () -> new PowerConsumptionInput(
				PowerInputMode.DIRECT_WATTAGE, 65.0, null, 120.0, 5.0, null, null, null, 5.0, null));
	}

	@Test
	void voltageCurrent_missingCurrentAmps_throws() {
		assertThrows(CalculationException.class, () -> new PowerConsumptionInput(
				PowerInputMode.VOLTAGE_CURRENT, null, CircuitType.SINGLE_PHASE_AC, 230.0, null, null, null, null, 2.0, null));
	}

	@Test
	void horsepower_motorEfficiencyZero_throws() {
		assertThrows(CalculationException.class, () -> horsepower(2.0, 0.0, 3.0));
	}

	@Test
	void horsepower_motorEfficiencyAbove100_throws() {
		assertThrows(CalculationException.class, () -> horsepower(2.0, 100.1, 3.0));
	}

	@Test
	void totalOperatingHoursNonPositive_throws() {
		assertThrows(CalculationException.class, () -> directWattage(65.0, 0.0, null));
	}

	@Test
	void pricePerKwhNonPositive_throws() {
		assertThrows(CalculationException.class, () -> directWattage(65.0, 5.0, 0.0));
	}

}
