package com.renzoproject.calc.core.smokecontrol;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.common.AirProperties;
import com.renzoproject.calc.core.common.AirPropertiesResolver;

/**
 * Required natural smoke vent area from a known smoke volumetric flow rate, via the standard
 * buoyancy-driven vent sizing formula: {@code Av = V / (Cd * sqrt(2*g*H*(deltaT/To_K)))}.
 *
 * <p>Deliberately does NOT call or depend on {@link SmokeProductionCalculator} or
 * {@link TSquaredSmokeProductionCalculator} -- {@code volumetricFlowRate}/{@code smokeTemperature}
 * are supplied directly by the caller (who may have gotten them from either of those calculators,
 * or from anywhere else), keeping this calculator usable standalone.
 *
 * <p>The {@code To_K = To + 273} conversion uses the same named-constant convention (not a bare
 * {@code 273} literal) as the Kelvin step in {@code SmokeProductionCalculator}/
 * {@code TSquaredSmokeProductionCalculator}'s density formulas -- each calculator keeps its own
 * copy of this constant rather than sharing one, consistent with this domain's established
 * "duplicate, don't share calculation logic" convention between its calculators.
 */
public class VentAreaCalculator implements Calculator<VentAreaInput, VentAreaResult> {

	private static final double CELSIUS_TO_KELVIN_OFFSET = 273.0;

	private final AirPropertiesResolver airPropertiesResolver;
	private final SmokeControlDefaultsResolver defaultsResolver;

	public VentAreaCalculator(AirPropertiesResolver airPropertiesResolver, SmokeControlDefaultsResolver defaultsResolver) {
		this.airPropertiesResolver = airPropertiesResolver;
		this.defaultsResolver = defaultsResolver;
	}

	@Override
	public VentAreaResult calculate(VentAreaInput input) {
		double dischargeCoefficient = input.dischargeCoefficient() != null
				? input.dischargeCoefficient()
				: defaultsResolver.defaults().ventDischargeCoefficient();

		AirProperties air = airPropertiesResolver.properties();
		double deltaT = input.smokeTemperature() - input.ambientTemperature();
		double ambientTemperatureKelvin = input.ambientTemperature() + CELSIUS_TO_KELVIN_OFFSET;

		double requiredVentArea = input.volumetricFlowRate()
				/ (dischargeCoefficient * Math.sqrt(2 * air.gravitationalAccelerationMPerS2() * input.ventHeight() * (deltaT / ambientTemperatureKelvin)));

		return new VentAreaResult(deltaT, requiredVentArea);
	}

}
