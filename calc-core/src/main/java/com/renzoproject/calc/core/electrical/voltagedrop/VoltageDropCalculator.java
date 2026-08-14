package com.renzoproject.calc.core.electrical.voltagedrop;

import com.renzoproject.calc.core.Calculator;

/**
 * Computes conductor voltage drop using the exact impedance method, which unifies DC,
 * single-phase AC, and three-phase AC in one formula:
 *
 * <ol>
 *   <li>{@code r}, {@code x} = per-conductor resistance/reactance divided by
 *       {@code parallelSetsPerPhase}</li>
 *   <li>DC: {@code effectiveImpedanceTerm = r} (power factor and reactance are ignored)</li>
 *   <li>AC: {@code effectiveImpedanceTerm = (r * powerFactor) + (x * sqrt(1 - powerFactor^2))}</li>
 *   <li>{@code phaseFactor = sqrt(3)} for three-phase, otherwise {@code 2.0} (covers DC and
 *       the single-phase return conductor)</li>
 *   <li>{@code voltageDropVolts = phaseFactor * oneWayLengthMeters * currentAmps *
 *       effectiveImpedanceTerm}</li>
 * </ol>
 */
public class VoltageDropCalculator implements Calculator<VoltageDropInput, VoltageDropResult> {

	/**
	 * PEC-recommended maximum voltage drop, as a percentage of system voltage.
	 * Used only to flag {@link VoltageDropResult#exceedsRecommendedLimit()}; never enforced
	 * by throwing.
	 */
	public static final double RECOMMENDED_MAX_PERCENT = 3.0;

	/**
	 * Calculates voltage drop for the given circuit parameters.
	 *
	 * @param input validated circuit parameters
	 * @return the computed voltage drop, percentage, receiving-end voltage, and whether the
	 *         result exceeds {@link #RECOMMENDED_MAX_PERCENT}
	 */
	@Override
	public VoltageDropResult calculate(VoltageDropInput input) {
		double r = input.resistanceOhmsPerMeter() / input.parallelSetsPerPhase();
		double x = input.reactanceOhmsPerMeter() / input.parallelSetsPerPhase();

		double effectiveImpedanceTerm;
		if (input.circuitType() == CircuitType.DC) {
			effectiveImpedanceTerm = r;
		} else {
			double powerFactor = input.powerFactor();
			double sinTheta = Math.sqrt(1 - powerFactor * powerFactor);
			effectiveImpedanceTerm = (r * powerFactor) + (x * sinTheta);
		}

		double phaseFactor = input.circuitType() == CircuitType.THREE_PHASE_AC ? Math.sqrt(3) : 2.0;

		double voltageDropVolts = phaseFactor * input.oneWayLengthMeters() * input.currentAmps() * effectiveImpedanceTerm;
		double voltageDropPercent = (voltageDropVolts / input.systemVoltage()) * 100.0;
		double receivingEndVoltage = input.systemVoltage() - voltageDropVolts;
		boolean exceedsRecommendedLimit = voltageDropPercent > RECOMMENDED_MAX_PERCENT;

		return new VoltageDropResult(voltageDropVolts, voltageDropPercent, receivingEndVoltage, exceedsRecommendedLimit);
	}

}
