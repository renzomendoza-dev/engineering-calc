package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * The two fixed rules behind laying out a remote area and starting a hydraulic calculation.
 *
 * @param standard                       what the rules come from
 * @param confidence                     {@code verified} only once checked against the printed
 *                                       standard; until then the plan says so in its notes
 * @param remoteAreaLengthFactor         the remote area's dimension parallel to the branch lines is
 *                                       at least this times the square root of the design area
 * @param minimumOperatingPressurePsi    the lowest pressure any sprinkler is taken to operate at
 */
public record RemoteAreaRules(
		String standard,
		String confidence,
		double remoteAreaLengthFactor,
		String remoteAreaLengthNote,
		double minimumOperatingPressurePsi,
		String minimumOperatingPressureNote) {

	public RemoteAreaRules {
		if (remoteAreaLengthFactor <= 0) {
			throw new CalculationException(
					"remoteAreaLengthFactor must be positive (was " + remoteAreaLengthFactor + ")");
		}
		if (minimumOperatingPressurePsi < 0) {
			throw new CalculationException(
					"minimumOperatingPressurePsi must not be negative (was " + minimumOperatingPressurePsi + ")");
		}
	}

	public boolean isVerified() {
		return "verified".equalsIgnoreCase(confidence);
	}

}
