package com.renzoproject.calc.core.mechanical.firepump;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;
import tech.units.indriya.quantity.Quantities;

import javax.measure.Quantity;
import javax.measure.quantity.Pressure;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes water/brake horsepower at the rated point (and, for {@link FullCurve}, the
 * interpolated 150%-of-rated point too), then resolves a recommended standard electric motor
 * size for the higher of those brake horsepower values.
 */
public class FirePumpPowerCalculator implements Calculator<FirePumpPowerInput, FirePumpPowerResult> {

	private static final double HEAD_FEET_PER_PSI = 2.31;
	private static final double WATER_HORSEPOWER_DIVISOR = 3960.0;
	private static final double OVERLOAD_FLOW_MULTIPLIER = 1.5;

	private final FirePumpMotorSizeResolver motorSizeResolver;

	public FirePumpPowerCalculator(FirePumpMotorSizeResolver motorSizeResolver) {
		this.motorSizeResolver = motorSizeResolver;
	}

	@Override
	public FirePumpPowerResult calculate(FirePumpPowerInput input) {
		List<PowerAtPoint> evaluatedPoints = new ArrayList<>();
		List<String> notes = new ArrayList<>();
		DriverType driverType;

		switch (input) {
			case RatedPointOnly ratedOnly -> {
				evaluatedPoints.add(evaluatePoint(ratedOnly.flow(), ratedOnly.pressure(), ratedOnly.pumpEfficiency(), ratedOnly.specificGravity()));
				driverType = ratedOnly.driverType();
				notes.add("rated-point-only: overload (150%) point not evaluated -- verify driver "
						+ "sizing against the pump's full curve before finalizing.");
			}
			case FullCurve fullCurve -> {
				CandidatePumpCurve curve = fullCurve.curve();
				evaluatedPoints.add(evaluatePoint(curve.ratedFlow(), curve.ratedPressure(), fullCurve.pumpEfficiency(), fullCurve.specificGravity()));

				double overloadFlowGpm = curve.ratedFlow().to(FirePumpUnits.GPM).getValue().doubleValue() * OVERLOAD_FLOW_MULTIPLIER;
				Quantity<VolumetricFlowRate> overloadFlow = Quantities.getQuantity(overloadFlowGpm, FirePumpUnits.GPM);
				Quantity<Pressure> overloadPressure = PumpCurveInterpolator.interpolatePressureAt(curve.curvePoints(), overloadFlow);
				evaluatedPoints.add(evaluatePoint(overloadFlow, overloadPressure, fullCurve.pumpEfficiency(), fullCurve.specificGravity()));

				driverType = fullCurve.driverType();
			}
		}

		double maxBrakeHorsepower = evaluatedPoints.stream().mapToDouble(PowerAtPoint::brakeHorsepower).max().orElseThrow();

		String recommendedElectricMotorSize;
		if (driverType == DriverType.ELECTRIC) {
			recommendedElectricMotorSize = formatHp(motorSizeResolver.resolveNextStandardMotorHp(maxBrakeHorsepower));
		} else {
			recommendedElectricMotorSize = null;
			notes.add("Diesel driver: no standard step table applies -- round brake horsepower up "
					+ "and consult the engine manufacturer's lineup for an available rating.");
		}

		String driverSizingNote = notes.isEmpty() ? null : String.join(" ", notes);

		return new FirePumpPowerResult(List.copyOf(evaluatedPoints), maxBrakeHorsepower, recommendedElectricMotorSize, driverSizingNote);
	}

	private PowerAtPoint evaluatePoint(Quantity<VolumetricFlowRate> flow, Quantity<Pressure> pressure, double pumpEfficiency, double specificGravity) {
		double flowGpm = flow.to(FirePumpUnits.GPM).getValue().doubleValue();
		double pressurePsi = pressure.to(FirePumpUnits.PSI).getValue().doubleValue();

		double headFeet = pressurePsi * HEAD_FEET_PER_PSI / specificGravity;
		double waterHorsepower = (flowGpm * headFeet * specificGravity) / WATER_HORSEPOWER_DIVISOR;
		double brakeHorsepower = waterHorsepower / pumpEfficiency;

		return new PowerAtPoint(flow, pressure, waterHorsepower, brakeHorsepower);
	}

	private static String formatHp(double hp) {
		String formatted = hp == Math.floor(hp) ? String.valueOf((long) hp) : String.valueOf(hp);
		return formatted + " HP";
	}

}
