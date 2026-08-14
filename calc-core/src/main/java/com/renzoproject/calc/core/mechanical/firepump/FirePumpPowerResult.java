package com.renzoproject.calc.core.mechanical.firepump;

import java.util.List;

/**
 * @param evaluatedPoints            just the rated point for {@link RatedPointOnly}; rated +
 *                                    150% point for {@link FullCurve}
 * @param maxBrakeHorsepower         the higher of {@code evaluatedPoints}' brake horsepower
 *                                    values — the basis for driver sizing
 * @param recommendedElectricMotorSize resolved via {@code motor-hp-steps.json}; {@code null}
 *                                    when the driver is diesel (no standard step table applies)
 * @param driverSizingNote           caveat text — e.g. a diesel-driver note, a rated-point-only
 *                                    "overload point not checked" warning, both, or
 *                                    {@code null} if neither applies
 */
public record FirePumpPowerResult(
		List<PowerAtPoint> evaluatedPoints,
		double maxBrakeHorsepower,
		String recommendedElectricMotorSize,
		String driverSizingNote) {

}
