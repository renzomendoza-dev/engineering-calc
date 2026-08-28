package com.renzoproject.calc.core.electrical.powerconsumption;

import com.renzoproject.calc.core.electrical.voltagedrop.CircuitType;
import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Input for {@link PowerConsumptionCalculator}. Which fields are required/forbidden depends on
 * {@link #mode()} -- a request mixing fields from the wrong mode is rejected outright rather than
 * silently ignoring the extra fields.
 *
 * @param mode                     how power draw is specified
 * @param wattageInput             required (and positive) for {@link PowerInputMode#DIRECT_WATTAGE};
 *                                 must be {@code null} otherwise
 * @param circuitType              required for {@link PowerInputMode#VOLTAGE_CURRENT}; must be
 *                                 {@code null} otherwise
 * @param voltage                  required (and positive) for {@link PowerInputMode#VOLTAGE_CURRENT};
 *                                 must be {@code null} otherwise
 * @param currentAmps              required (and positive) for {@link PowerInputMode#VOLTAGE_CURRENT};
 *                                 must be {@code null} otherwise
 * @param powerFactor              only meaningful for {@link PowerInputMode#VOLTAGE_CURRENT};
 *                                 optional there (defaults to {@code 1.0}), must be in
 *                                 {@code (0, 1]} if provided, and must be {@code null} for
 *                                 {@link CircuitType#DC} (no power factor concept); must be
 *                                 {@code null} for the other two modes
 * @param horsepowerInput          required (and positive) for {@link PowerInputMode#HORSEPOWER};
 *                                 must be {@code null} otherwise
 * @param motorEfficiencyPercent   only meaningful for {@link PowerInputMode#HORSEPOWER}; optional
 *                                 there (defaults to {@code 85.0}), must be in {@code (0, 100]} if
 *                                 provided; must be {@code null} for the other two modes
 * @param totalOperatingHours      always required, must be positive
 * @param pricePerKwh              always optional; must be positive if provided
 * @throws CalculationException if any of the above rules is violated
 */
public record PowerConsumptionInput(
		PowerInputMode mode,
		Double wattageInput,
		CircuitType circuitType,
		Double voltage,
		Double currentAmps,
		Double powerFactor,
		Double horsepowerInput,
		Double motorEfficiencyPercent,
		double totalOperatingHours,
		Double pricePerKwh) {

	public PowerConsumptionInput {
		if (mode == null) {
			throw new CalculationException("mode is required");
		}

		switch (mode) {
			case DIRECT_WATTAGE -> validateDirectWattage(wattageInput, circuitType, voltage, currentAmps, powerFactor,
					horsepowerInput, motorEfficiencyPercent);
			case VOLTAGE_CURRENT -> validateVoltageCurrent(wattageInput, circuitType, voltage, currentAmps, powerFactor,
					horsepowerInput, motorEfficiencyPercent);
			case HORSEPOWER -> validateHorsepower(wattageInput, circuitType, voltage, currentAmps, powerFactor,
					horsepowerInput, motorEfficiencyPercent);
		}

		if (totalOperatingHours <= 0) {
			throw new CalculationException("totalOperatingHours must be positive");
		}
		if (pricePerKwh != null && pricePerKwh <= 0) {
			throw new CalculationException("pricePerKwh must be positive if provided");
		}
	}

	private static void validateDirectWattage(
			Double wattageInput, CircuitType circuitType, Double voltage, Double currentAmps, Double powerFactor,
			Double horsepowerInput, Double motorEfficiencyPercent) {
		if (wattageInput == null || wattageInput <= 0) {
			throw new CalculationException("wattageInput must be positive for DIRECT_WATTAGE mode");
		}
		if (circuitType != null || voltage != null || currentAmps != null || powerFactor != null
				|| horsepowerInput != null || motorEfficiencyPercent != null) {
			throw new CalculationException("circuitType, voltage, currentAmps, powerFactor, horsepowerInput, and "
					+ "motorEfficiencyPercent must all be null for DIRECT_WATTAGE mode");
		}
	}

	private static void validateVoltageCurrent(
			Double wattageInput, CircuitType circuitType, Double voltage, Double currentAmps, Double powerFactor,
			Double horsepowerInput, Double motorEfficiencyPercent) {
		if (wattageInput != null || horsepowerInput != null || motorEfficiencyPercent != null) {
			throw new CalculationException("wattageInput, horsepowerInput, and motorEfficiencyPercent must all be "
					+ "null for VOLTAGE_CURRENT mode");
		}
		if (circuitType == null) {
			throw new CalculationException("circuitType is required for VOLTAGE_CURRENT mode");
		}
		if (voltage == null || voltage <= 0) {
			throw new CalculationException("voltage must be positive for VOLTAGE_CURRENT mode");
		}
		if (currentAmps == null || currentAmps <= 0) {
			throw new CalculationException("currentAmps must be positive for VOLTAGE_CURRENT mode");
		}
		if (circuitType == CircuitType.DC) {
			if (powerFactor != null) {
				throw new CalculationException("powerFactor must be null for DC circuits (no power factor concept)");
			}
		} else if (powerFactor != null && (powerFactor <= 0 || powerFactor > 1)) {
			throw new CalculationException("powerFactor must be in (0, 1] if provided");
		}
	}

	private static void validateHorsepower(
			Double wattageInput, CircuitType circuitType, Double voltage, Double currentAmps, Double powerFactor,
			Double horsepowerInput, Double motorEfficiencyPercent) {
		if (horsepowerInput == null || horsepowerInput <= 0) {
			throw new CalculationException("horsepowerInput must be positive for HORSEPOWER mode");
		}
		if (wattageInput != null || circuitType != null || voltage != null || currentAmps != null || powerFactor != null) {
			throw new CalculationException("wattageInput, circuitType, voltage, currentAmps, and powerFactor must "
					+ "all be null for HORSEPOWER mode");
		}
		if (motorEfficiencyPercent != null && (motorEfficiencyPercent <= 0 || motorEfficiencyPercent > 100)) {
			throw new CalculationException("motorEfficiencyPercent must be in (0, 100] if provided");
		}
	}

}
