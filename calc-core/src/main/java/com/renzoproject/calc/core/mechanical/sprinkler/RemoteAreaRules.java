package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * The two fixed rules behind laying out a remote area and starting a hydraulic calculation.
 *
 * @param standard                       what the rules come from
 * @param confidence                     {@code verified} only once every rule below has been
 *                                       checked against the printed standard; until then the plan
 *                                       says which are still open
 * @param remoteAreaLengthConfidence     confidence in the length factor alone
 * @param remoteAreaLengthSource         where the length factor comes from, with its section
 * @param minimumOperatingPressureConfidence confidence in the minimum operating pressure alone
 * @param minimumOperatingPressureSource where the minimum comes from, with its section
 * @param maximumOperatingPressurePsi    the highest pressure a sprinkler is allowed to operate at
 *                                       (extra hazard and storage); {@code 0} when not given
 * @param remoteAreaLengthFactor         the remote area's dimension parallel to the branch lines is
 *                                       at least this times the square root of the design area
 * @param minimumOperatingPressurePsi    the lowest pressure any sprinkler is taken to operate at
 */
public record RemoteAreaRules(
		String standard,
		String confidence,
		double remoteAreaLengthFactor,
		String remoteAreaLengthNote,
		String remoteAreaLengthConfidence,
		String remoteAreaLengthSource,
		double minimumOperatingPressurePsi,
		String minimumOperatingPressureNote,
		String minimumOperatingPressureConfidence,
		String minimumOperatingPressureSource,
		double maximumOperatingPressurePsi,
		String maximumOperatingPressureSource,
		String maximumOperatingPressureNote) {

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

	public boolean isRemoteAreaLengthVerified() {
		return "verified".equalsIgnoreCase(remoteAreaLengthConfidence);
	}

	public boolean isMinimumOperatingPressureVerified() {
		return "verified".equalsIgnoreCase(minimumOperatingPressureConfidence);
	}

}
