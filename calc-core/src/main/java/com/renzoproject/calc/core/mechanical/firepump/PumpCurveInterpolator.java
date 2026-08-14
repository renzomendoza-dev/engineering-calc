package com.renzoproject.calc.core.mechanical.firepump;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;
import tech.units.indriya.quantity.Quantities;

import javax.measure.Quantity;
import javax.measure.quantity.Pressure;
import java.util.Comparator;
import java.util.List;

/**
 * Linear interpolation between {@link PumpCurvePoint}s, shared by
 * {@link FirePumpCurveValidationCalculator} (churn @ 0%, overload @ the loaded overload
 * percentage) and {@link FirePumpPowerCalculator}'s {@code FullCurve} mode (its own 150% point)
 * — factored out here rather than duplicated in both, per the pipe velocity calculator's
 * "no duplicated lookup/interpolation logic" precedent.
 */
final class PumpCurveInterpolator {

	private static final double EPSILON = 1e-9;

	private PumpCurveInterpolator() {
	}

	/**
	 * @throws CalculationException if {@code curvePoints} doesn't bracket {@code targetFlow} —
	 *                              this method never extrapolates beyond the provided curve data
	 */
	static Quantity<Pressure> interpolatePressureAt(List<PumpCurvePoint> curvePoints, Quantity<VolumetricFlowRate> targetFlow) {
		List<PumpCurvePoint> sorted = curvePoints.stream()
				.sorted(Comparator.comparingDouble(point -> point.flow().to(FirePumpUnits.GPM).getValue().doubleValue()))
				.toList();

		double targetGpm = targetFlow.to(FirePumpUnits.GPM).getValue().doubleValue();
		double minGpm = sorted.get(0).flow().to(FirePumpUnits.GPM).getValue().doubleValue();
		double maxGpm = sorted.get(sorted.size() - 1).flow().to(FirePumpUnits.GPM).getValue().doubleValue();

		if (targetGpm < minGpm - EPSILON || targetGpm > maxGpm + EPSILON) {
			throw new CalculationException("curvePoints do not bracket " + targetGpm + " GPM (curve spans "
					+ minGpm + " to " + maxGpm + " GPM) -- cannot extrapolate beyond provided curve data");
		}

		for (int i = 0; i < sorted.size() - 1; i++) {
			double f1 = sorted.get(i).flow().to(FirePumpUnits.GPM).getValue().doubleValue();
			double f2 = sorted.get(i + 1).flow().to(FirePumpUnits.GPM).getValue().doubleValue();
			if (targetGpm >= f1 - EPSILON && targetGpm <= f2 + EPSILON) {
				double p1 = sorted.get(i).pressure().to(FirePumpUnits.PSI).getValue().doubleValue();
				double p2 = sorted.get(i + 1).pressure().to(FirePumpUnits.PSI).getValue().doubleValue();
				double interpolatedPsi = Math.abs(f2 - f1) < EPSILON
						? p1
						: p1 + ((targetGpm - f1) / (f2 - f1)) * (p2 - p1);
				return Quantities.getQuantity(interpolatedPsi, FirePumpUnits.PSI);
			}
		}

		throw new CalculationException("Unable to bracket " + targetGpm + " GPM within curvePoints");
	}

}
