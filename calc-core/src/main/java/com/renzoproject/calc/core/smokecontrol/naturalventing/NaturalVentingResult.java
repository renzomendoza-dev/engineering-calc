package com.renzoproject.calc.core.smokecontrol.naturalventing;

import com.renzoproject.calc.core.compliance.ComplianceCheck;
import java.util.List;

/**
 * Result of {@link NaturalVentingCalculator}. Figures are plain SI doubles (m, m2, kW, kW/m2, K,
 * kg/s, kg/m3, m/s), like the rest of the smoke control package.
 *
 * @param fireCases one per input fire case, in input order
 * @param zones     one per input zone, in input order; the checks that do not depend on a fire
 */
public record NaturalVentingResult(
		List<FireCaseResult> fireCases,
		List<ZoneResult> zones) {

	/** Which plume equation applied (9.2.3.6 or 9.2.3.7). */
	public enum Regime {

		/** The mean flame height is below the smoke layer boundary, {@code L < zs} (9.2.3.6). */
		BELOW_LAYER,

		/** The flames reach the smoke layer boundary, {@code L >= zs} (9.2.3.7). */
		REACHES_LAYER

	}

	/**
	 * The supplementary time-stepping check of the smoke layer boundary (not NFPA 204).
	 *
	 * @param endTime                       tr + td, s: the time the boundary is read at
	 * @param interfaceHeightAtEnd          height of the layer boundary above the floor at {@code endTime}, m
	 * @param timeToDesignHeight            time from ignition at which the boundary first falls below the design
	 *                                      height with the vents working, s; null if it does not within {@code horizon}
	 * @param timeToDesignHeightWithoutVents the same with no venting, s; null if it does not within {@code horizon}
	 * @param horizon                       time stepped to, s
	 */
	public record TransientInterface(
			double endTime,
			double interfaceHeightAtEnd,
			Double timeToDesignHeight,
			Double timeToDesignHeightWithoutVents,
			double horizon) {

	}

	/**
	 * The smoke layer of a fire against a wall or in a corner by the image-fire method (not NFPA 204): the fire
	 * is treated as {@code multiplier} times its size in open space, and the plume flow divided by it.
	 *
	 * @param multiplier                2 for a wall fire, 4 for a corner fire
	 * @param equivalentHeatReleaseRate multiplier x Q, kW
	 * @param plumeMassFlow             mp of the fire against the wall or in the corner, kg/s
	 * @param layerTemperatureRise      dT = K Qc / (cp mp), K
	 * @param layerTemperature          T, K
	 */
	public record ImageFireScenario(
			double multiplier,
			double equivalentHeatReleaseRate,
			double plumeMassFlow,
			double layerTemperatureRise,
			double layerTemperature) {

	}

	/**
	 * @param wall   the fire against a wall
	 * @param corner the fire in a corner
	 */
	public record ImageFire(ImageFireScenario wall, ImageFireScenario corner) {

	}

	/**
	 * @param name                      the fire case
	 * @param zoneName                  its zone
	 * @param designHeatReleaseRate     Q, kW
	 * @param heatReleaseRateCapped     whether Q was limited to the fire's maximum heat release rate
	 * @param growthCoefficient         alpha_g, kW/s2, for a growing fire; otherwise null
	 * @param detectionTime             td, s, used for a growing fire; null for a steady fire
	 * @param detectionTimeFromDetector whether td was worked out from the smoke detector equation (9.2.5.4.3), not given
	 * @param fireDiameter              D, m
	 * @param meanFlameHeight           L, m (9.2.3.1)
	 * @param virtualOrigin             z0, m (9.2.3.2)
	 * @param layerHeightAboveFire      zs, m
	 * @param layerDepth                d, m: from the layer boundary to the middle of the vents
	 * @param regime                    plume equation used
	 * @param lineFireFactor            {@code [4Wf/zs]^(2/3)} when applied (9.2.3.5), else 1
	 * @param convectiveHeatReleaseRate Qc = 0.7 Q, kW
	 * @param plumeMassFlow             mp, kg/s
	 * @param layerTemperatureRise      dT, K (9.2.4.3)
	 * @param layerTemperature          T, K
	 * @param layerDensity              rho_s, kg/m3 (ideal gas, relative to ambient)
	 * @param aerodynamicVentArea       Cd,v Av without any inlet restriction, m2 (Annex D: Aa)
	 * @param requiredVentArea          geometric vent area Av required with the inlet taken into
	 *                                  account (Eq. 9.2.4.1), m2; null when the inlet is too small
	 *                                  to pass the plume flow
	 * @param inletCorrection           M = required area with the inlet / without it; null when no
	 *                                  inlet is defined or it is too small
	 * @param requiredInletArea         inlet area for the chosen ratio, m2; null unless in design mode
	 * @param inletAreaForVelocityLimit inlet area giving 1 m/s at the plume mass flow, m2 (6.6.3)
	 * @param inletVelocity             mp / (rho_o Ai), m/s; null when no inlet area is known
	 * @param floorRadiantFlux          sigma T^4 at floor level, kW/m2 (Annex D)
	 * @param feasibleHeatReleaseRate   12,000 zs^(5/2), kW (Equation 1.3.5), the fire above which the standard is not valid
	 * @param minimumAisleWidth         0.042 Q^(1/2), m (8.2.3), steady fires only; else null
	 * @param checks                    compliance lines for this case
	 * @param imageFire                 the layer for a fire against a wall and in a corner, supplementary
	 * @param transientInterface        the time-stepping check; null for a steady fire or when the zone lacks
	 *                                  the geometry it needs
	 */
	public record FireCaseResult(
			String name,
			String zoneName,
			double designHeatReleaseRate,
			boolean heatReleaseRateCapped,
			Double growthCoefficient,
			Double detectionTime,
			boolean detectionTimeFromDetector,
			double fireDiameter,
			double meanFlameHeight,
			double virtualOrigin,
			double layerHeightAboveFire,
			double layerDepth,
			Regime regime,
			double lineFireFactor,
			double convectiveHeatReleaseRate,
			double plumeMassFlow,
			double layerTemperatureRise,
			double layerTemperature,
			double layerDensity,
			double aerodynamicVentArea,
			Double requiredVentArea,
			Double inletCorrection,
			Double requiredInletArea,
			double inletAreaForVelocityLimit,
			Double inletVelocity,
			double floorRadiantFlux,
			double feasibleHeatReleaseRate,
			Double minimumAisleWidth,
			List<ComplianceCheck> checks,
			ImageFire imageFire,
			TransientInterface transientInterface) {

	}

	/**
	 * @param name   the zone
	 * @param checks geometry and spacing checks that do not depend on the fire
	 */
	public record ZoneResult(String name, List<ComplianceCheck> checks) {

	}

}
