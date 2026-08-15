package com.renzoproject.calc.core.mechanical.pump;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.mechanical.pipe.PipeUnits;
import tech.units.indriya.unit.Units;

/**
 * Hydraulic/shaft power and electric motor sizing from a known duty point (flow + TDH).
 * Standalone — doesn't depend on {@link PumpTDHCalculator}, usable directly if the caller
 * already knows their duty point.
 */
public class PumpPowerCalculator implements Calculator<PumpPowerInput, PumpPowerResult> {

	private static final double GRAVITY_M_S2 = 9.80665;

	private final PumpMotorSizeResolver motorSizeResolver;

	public PumpPowerCalculator(PumpMotorSizeResolver motorSizeResolver) {
		this.motorSizeResolver = motorSizeResolver;
	}

	@Override
	public PumpPowerResult calculate(PumpPowerInput input) {
		double flowRateM3s = input.flowRate().to(PipeUnits.CUBIC_METRE_PER_SECOND).getValue().doubleValue();
		double totalDynamicHeadM = input.totalDynamicHead().to(Units.METRE).getValue().doubleValue();

		double hydraulicPowerW = input.fluidDensityKgM3() * GRAVITY_M_S2 * flowRateM3s * totalDynamicHeadM;
		double hydraulicPowerKw = hydraulicPowerW / 1000.0;
		double shaftPowerKw = hydraulicPowerKw / input.pumpEfficiency();

		double resolvedKw = motorSizeResolver.resolveNextStandardMotorKw(shaftPowerKw);
		String recommendedMotorSizeKw = formatKw(resolvedKw);

		return new PumpPowerResult(hydraulicPowerKw, shaftPowerKw, recommendedMotorSizeKw);
	}

	private static String formatKw(double kw) {
		String formatted = kw == Math.floor(kw) ? String.valueOf((long) kw) : String.valueOf(kw);
		return formatted + " kW";
	}

}
