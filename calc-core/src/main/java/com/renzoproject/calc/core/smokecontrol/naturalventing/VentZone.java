package com.renzoproject.calc.core.smokecontrol.naturalventing;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * A curtained area (smoke reservoir) that is vented, with its geometry and the vents and inlets
 * provided. Every figure that is only needed for a check is nullable: the check then reports
 * {@link CheckStatus#OPEN} instead of failing.
 *
 * @param name                      label the fire cases refer to; not blank
 * @param ceilingHeight             H, m, floor to ceiling (7.3.1); positive
 * @param ventCentreHeight          m above the floor to the middle of the vent openings, used for
 *                                  the smoke layer depth {@code d}; nullable, defaults to the ceiling
 * @param curtainDepth              depth of the draft curtains, m; nullable (no curtains, or unknown)
 * @param zoneLength                length of the curtained area, m; nullable
 * @param zoneWidth                 width of the curtained area, m; nullable
 * @param ventDischargeCoefficient  Cd,v of the vents (Table 9.2.4.2 or the manufacturer); in (0, 1]
 * @param ventCount                 number of vents; nullable
 * @param ventTotalArea             total geometric vent area provided, m2; nullable
 * @param ventUnitLength            length of one vent, m; nullable
 * @param ventUnitWidth             width of one vent, m; nullable
 * @param ventSpacing               largest centre-to-centre vent spacing S, m; nullable
 * @param ventToWallDistance        largest horizontal distance from a point on a wall or curtain to
 *                                  the nearest vent centre, m; nullable
 * @param inletDischargeCoefficient Cd,i of the inlets; nullable, defaults to {@code ventDischargeCoefficient}
 * @param inletAreaRatio            design mode: inlet area as a multiple of the vent area (Annex D
 *                                  uses 1.5); nullable. Ignored when {@code inletNetArea} is given
 * @param inletNetArea              check mode: net free inlet area provided, m2; nullable
 * @param maxLayerTemperatureRise   design objective, K above ambient (clause 4.1); nullable
 * @param maxFloorRadiantFlux       design objective, kW/m2 at floor level (Annex D uses 2.5); nullable
 * @param aisleWidth                aisle width separating fuel arrays, m, for 8.2.3; nullable
 */
public record VentZone(
		String name,
		double ceilingHeight,
		Double ventCentreHeight,
		Double curtainDepth,
		Double zoneLength,
		Double zoneWidth,
		double ventDischargeCoefficient,
		Integer ventCount,
		Double ventTotalArea,
		Double ventUnitLength,
		Double ventUnitWidth,
		Double ventSpacing,
		Double ventToWallDistance,
		Double inletDischargeCoefficient,
		Double inletAreaRatio,
		Double inletNetArea,
		Double maxLayerTemperatureRise,
		Double maxFloorRadiantFlux,
		Double aisleWidth) {

	public VentZone {
		if (name == null || name.isBlank()) {
			throw new CalculationException("zone name must not be blank");
		}
		if (ceilingHeight <= 0) {
			throw new CalculationException("zone " + name + ": ceilingHeight must be positive");
		}
		if (ventCentreHeight == null) {
			ventCentreHeight = ceilingHeight;
		}
		if (ventCentreHeight <= 0 || ventCentreHeight > ceilingHeight) {
			throw new CalculationException("zone " + name + ": ventCentreHeight must be above the floor and not above the ceiling");
		}
		if (ventDischargeCoefficient <= 0 || ventDischargeCoefficient > 1) {
			throw new CalculationException("zone " + name + ": ventDischargeCoefficient must be greater than 0 and at most 1");
		}
		if (inletDischargeCoefficient == null) {
			inletDischargeCoefficient = ventDischargeCoefficient;
		}
		if (inletDischargeCoefficient <= 0 || inletDischargeCoefficient > 1) {
			throw new CalculationException("zone " + name + ": inletDischargeCoefficient must be greater than 0 and at most 1");
		}
		requirePositiveWhenGiven(name, "curtainDepth", curtainDepth);
		requirePositiveWhenGiven(name, "zoneLength", zoneLength);
		requirePositiveWhenGiven(name, "zoneWidth", zoneWidth);
		requirePositiveWhenGiven(name, "ventTotalArea", ventTotalArea);
		requirePositiveWhenGiven(name, "ventUnitLength", ventUnitLength);
		requirePositiveWhenGiven(name, "ventUnitWidth", ventUnitWidth);
		requirePositiveWhenGiven(name, "ventSpacing", ventSpacing);
		requirePositiveWhenGiven(name, "ventToWallDistance", ventToWallDistance);
		requirePositiveWhenGiven(name, "inletAreaRatio", inletAreaRatio);
		requirePositiveWhenGiven(name, "inletNetArea", inletNetArea);
		requirePositiveWhenGiven(name, "maxLayerTemperatureRise", maxLayerTemperatureRise);
		requirePositiveWhenGiven(name, "maxFloorRadiantFlux", maxFloorRadiantFlux);
		requirePositiveWhenGiven(name, "aisleWidth", aisleWidth);
		if (ventCount != null && ventCount <= 0) {
			throw new CalculationException("zone " + name + ": ventCount must be positive");
		}
		if (curtainDepth != null && curtainDepth > ceilingHeight) {
			throw new CalculationException("zone " + name + ": curtainDepth must not exceed ceilingHeight");
		}
	}

	private static void requirePositiveWhenGiven(String zone, String field, Double value) {
		if (value != null && value <= 0) {
			throw new CalculationException("zone " + zone + ": " + field + " must be positive");
		}
	}

}
