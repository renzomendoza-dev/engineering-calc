package com.renzoproject.calc.core.smokecontrol.naturalventing;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Detection time of a smoke detector in a continuously growing t-squared fire, NFPA 204 (2021)
 * Equation 9.2.5.4.3. A smoke detector is taken to respond at a certain gas temperature rise,
 * {@code dT}, at its location (9.2.5.4.3):
 *
 * <pre>
 *   dT = 3575 / (tg^(4/5) H^(3/5)) * [ (t / (tg^(2/5) H^(4/5)) - 0.442 (1 + r/H)) / (1 + 1.65 r/H) ]^(4/3)
 * </pre>
 *
 * <p>with {@code dT} taken as zero while the numerator of the bracket is zero or negative. Units:
 * seconds, metres, degrees Celsius (a rise, so also kelvin). {@code tg} is the fire growth time
 * (time to 1055 kW), {@code H} the ceiling height above the base of the fire and {@code r} the
 * radius from the fire axis to the detector. The time is from effective ignition, as in the
 * t-squared design fire of 8.3.
 *
 * <p>This is the printed hand-calculation equation. It is not the DETACT-QS program the Annex D
 * sample problem used (A.9.2.5.4.4.2), so its result for the same detector can differ a little from
 * the time that example prints.
 */
public final class SmokeDetectorDetectionTime {

	/** Minimum temperature rise for activation where there are no test data for the detector and fuel (9.2.5.4.3.2), degrees C. */
	public static final double MINIMUM_TEMPERATURE_RISE = 20.0;

	private static final double COEFFICIENT = 3575.0;
	private static final double ARRIVAL_COEFFICIENT = 0.442;
	private static final double RADIUS_COEFFICIENT = 1.65;

	private SmokeDetectorDetectionTime() {
	}

	/**
	 * The gas temperature rise at radius {@code r} and time {@code t} (Equation 9.2.5.4.3).
	 *
	 * @param growthTime  tg, s
	 * @param height      H, ceiling height above the base of the fire, m
	 * @param radius      r, from the fire axis, m; not negative
	 * @param time        t, s from effective ignition
	 * @return the rise, degrees C; zero before the plume has reached the detector
	 */
	public static double temperatureRise(double growthTime, double height, double radius, double time) {
		requireInputs(growthTime, height, radius);
		double numerator = time / (Math.pow(growthTime, 0.4) * Math.pow(height, 0.8)) - ARRIVAL_COEFFICIENT * (1 + radius / height);
		if (numerator <= 0) {
			return 0.0;
		}
		double bracket = numerator / (1 + RADIUS_COEFFICIENT * radius / height);
		return COEFFICIENT / (Math.pow(growthTime, 0.8) * Math.pow(height, 0.6)) * Math.pow(bracket, 4.0 / 3.0);
	}

	/**
	 * The time at which the gas temperature rise at radius {@code r} first reaches {@code dT}: the
	 * equation solved for {@code t}, which is exact because the rise only increases with time.
	 *
	 * @param growthTime      tg, s
	 * @param height          H, ceiling height above the base of the fire, m
	 * @param radius          r, from the fire axis to the detector, m; not negative
	 * @param temperatureRise dT, the rise at which the detector activates, degrees C; positive
	 * @return td, s from effective ignition
	 */
	public static double timeToTemperatureRise(double growthTime, double height, double radius, double temperatureRise) {
		requireInputs(growthTime, height, radius);
		if (temperatureRise <= 0) {
			throw new CalculationException("detector temperature rise must be positive");
		}
		double bracket = Math.pow(temperatureRise * Math.pow(growthTime, 0.8) * Math.pow(height, 0.6) / COEFFICIENT, 3.0 / 4.0);
		double numerator = bracket * (1 + RADIUS_COEFFICIENT * radius / height);
		return Math.pow(growthTime, 0.4) * Math.pow(height, 0.8) * (numerator + ARRIVAL_COEFFICIENT * (1 + radius / height));
	}

	private static void requireInputs(double growthTime, double height, double radius) {
		if (growthTime <= 0) {
			throw new CalculationException("growth time must be positive");
		}
		if (height <= 0) {
			throw new CalculationException("ceiling height above the base of the fire must be positive");
		}
		if (radius < 0) {
			throw new CalculationException("detector distance from the fire axis must not be negative");
		}
	}

}
