package com.renzoproject.calc.core.smokecontrol.naturalventing;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * One design fire in one {@link VentZone}, evaluated at one design smoke layer height.
 *
 * <p>The fire's base diameter {@code D} comes from exactly one of {@code fireDiameter},
 * {@code fireArea} (8.2.4: diameter of the circle of the same area) or
 * {@code unitHeatReleaseRate} (8.3.7: {@code D = (4Q / (pi Q''))^0.5}).
 *
 * @param name                name shown in results; not blank
 * @param zoneName            the {@link VentZone} it is in; not blank
 * @param kind                steady or growing
 * @param steadyHeatReleaseRate Q, kW; required for {@link FireCaseKind#STEADY}
 * @param growthTime          tg, s, time to 1055 kW (1 MW); required for {@link FireCaseKind#GROWING}
 * @param detectionTime       td, s; required for {@link FireCaseKind#GROWING}
 * @param designInterval      tr, s, measured from detection (3.3.8); required for {@link FireCaseKind#GROWING}
 * @param maxHeatReleaseRate  fuel-limited ceiling for a growing fire, kW; nullable
 * @param fireDiameter        D, m; nullable
 * @param fireArea            floor area of the fuel, m2; nullable
 * @param unitHeatReleaseRate Q'', kW/m2 of floor area; nullable
 * @param largestFireDimension Wf, m, only for a long narrow line-like fire (9.2.3.4); nullable
 * @param fireBaseHeight      height of the base of the fire above the floor, m; nullable, defaults to 0
 * @param smokeLayerHeight    design height of the smoke layer boundary above the floor, m; positive
 */
public record FireCase(
		String name,
		String zoneName,
		FireCaseKind kind,
		Double steadyHeatReleaseRate,
		Double growthTime,
		Double detectionTime,
		Double designInterval,
		Double maxHeatReleaseRate,
		Double fireDiameter,
		Double fireArea,
		Double unitHeatReleaseRate,
		Double largestFireDimension,
		Double fireBaseHeight,
		double smokeLayerHeight) {

	public FireCase {
		if (name == null || name.isBlank()) {
			throw new CalculationException("fire case name must not be blank");
		}
		if (zoneName == null || zoneName.isBlank()) {
			throw new CalculationException("fire case " + name + ": zoneName must not be blank");
		}
		if (kind == null) {
			throw new CalculationException("fire case " + name + ": kind is required");
		}
		switch (kind) {
			case STEADY -> {
				requirePositive(name, "steadyHeatReleaseRate", steadyHeatReleaseRate);
			}
			case GROWING -> {
				requirePositive(name, "growthTime", growthTime);
				requirePositive(name, "designInterval", designInterval);
				if (detectionTime == null || detectionTime < 0) {
					throw new CalculationException("fire case " + name + ": detectionTime is required and must not be negative");
				}
			}
		}
		if (maxHeatReleaseRate != null && maxHeatReleaseRate <= 0) {
			throw new CalculationException("fire case " + name + ": maxHeatReleaseRate must be positive");
		}
		int diameterSources = (fireDiameter != null ? 1 : 0) + (fireArea != null ? 1 : 0) + (unitHeatReleaseRate != null ? 1 : 0);
		if (diameterSources != 1) {
			throw new CalculationException("fire case " + name
					+ ": give exactly one of fireDiameter, fireArea or unitHeatReleaseRate");
		}
		if (fireDiameter != null) {
			requirePositive(name, "fireDiameter", fireDiameter);
		}
		if (fireArea != null) {
			requirePositive(name, "fireArea", fireArea);
		}
		if (unitHeatReleaseRate != null) {
			requirePositive(name, "unitHeatReleaseRate", unitHeatReleaseRate);
		}
		if (largestFireDimension != null) {
			requirePositive(name, "largestFireDimension", largestFireDimension);
		}
		if (fireBaseHeight == null) {
			fireBaseHeight = 0.0;
		}
		if (fireBaseHeight < 0) {
			throw new CalculationException("fire case " + name + ": fireBaseHeight must not be negative");
		}
		if (smokeLayerHeight <= fireBaseHeight) {
			throw new CalculationException("fire case " + name + ": smokeLayerHeight must be above the base of the fire");
		}
	}

	private static void requirePositive(String fireCase, String field, Double value) {
		if (value == null || value <= 0) {
			throw new CalculationException("fire case " + fireCase + ": " + field + " is required and must be positive");
		}
	}

}
