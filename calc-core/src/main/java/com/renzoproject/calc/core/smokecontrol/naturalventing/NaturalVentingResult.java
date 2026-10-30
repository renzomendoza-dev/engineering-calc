package com.renzoproject.calc.core.smokecontrol.naturalventing;

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
	 * @param name                      the fire case
	 * @param zoneName                  its zone
	 * @param designHeatReleaseRate     Q, kW
	 * @param heatReleaseRateCapped     whether Q was limited to the fire's maximum heat release rate
	 * @param growthCoefficient         alpha_g, kW/s2, for a growing fire; otherwise null
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
	 * @param feasibleHeatReleaseRate   23,200 zs^(5/2), kW (Annex D, D.7h)
	 * @param minimumAisleWidth         0.042 Q^(1/2), m (8.2.3), steady fires only; else null
	 * @param checks                    compliance lines for this case
	 */
	public record FireCaseResult(
			String name,
			String zoneName,
			double designHeatReleaseRate,
			boolean heatReleaseRateCapped,
			Double growthCoefficient,
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
			List<VentingCheck> checks) {

	}

	/**
	 * @param name   the zone
	 * @param checks geometry and spacing checks that do not depend on the fire
	 */
	public record ZoneResult(String name, List<VentingCheck> checks) {

	}

}
