package com.renzoproject.calc.core.electrical.voltagedrop;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Input parameters for {@link VoltageDropCalculator}.
 *
 * <p>{@code resistanceOhmsPerMeter} and {@code reactanceOhmsPerMeter} describe a single
 * conductor and are expected to already be temperature-corrected (e.g. resolved from a
 * PEC ampacity/conductor reference table upstream) — this type does no table lookups.
 *
 * @param circuitType              topology of the circuit; determines whether reactance and
 *                                 power factor apply
 * @param currentAmps              load current, in amperes; must be positive
 * @param oneWayLengthMeters       one-way conductor run length, in meters; must be positive
 * @param resistanceOhmsPerMeter   resistance per meter of a single conductor, in ohms/meter;
 *                                 must be positive
 * @param reactanceOhmsPerMeter    reactance per meter of a single conductor, in ohms/meter;
 *                                 must be {@code 0} for {@link CircuitType#DC}
 * @param powerFactor              load power factor; ignored for {@link CircuitType#DC},
 *                                 otherwise must be in {@code (0, 1]}
 * @param systemVoltage            nominal system voltage, in volts; must be positive
 * @param parallelSetsPerPhase     number of conductors run in parallel per phase; must be
 *                                 at least {@code 1}
 * @throws CalculationException if any validation rule above is violated
 */
public record VoltageDropInput(
		CircuitType circuitType,
		double currentAmps,
		double oneWayLengthMeters,
		double resistanceOhmsPerMeter,
		double reactanceOhmsPerMeter,
		double powerFactor,
		double systemVoltage,
		int parallelSetsPerPhase) {

	public VoltageDropInput {
		if (currentAmps <= 0) {
			throw new CalculationException("currentAmps must be positive");
		}
		if (oneWayLengthMeters <= 0) {
			throw new CalculationException("oneWayLengthMeters must be positive");
		}
		if (resistanceOhmsPerMeter <= 0) {
			throw new CalculationException("resistanceOhmsPerMeter must be positive");
		}
		if (systemVoltage <= 0) {
			throw new CalculationException("systemVoltage must be positive");
		}
		if (parallelSetsPerPhase < 1) {
			throw new CalculationException("parallelSetsPerPhase must be at least 1");
		}
		if (circuitType == CircuitType.DC) {
			if (reactanceOhmsPerMeter != 0) {
				throw new CalculationException("reactanceOhmsPerMeter must be 0 for DC circuits");
			}
		} else if (powerFactor <= 0 || powerFactor > 1) {
			throw new CalculationException("powerFactor must be in (0, 1] for AC circuits");
		}
	}

}
