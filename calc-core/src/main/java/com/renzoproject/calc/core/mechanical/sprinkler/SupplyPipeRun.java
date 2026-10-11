package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;

import java.util.List;

/**
 * One pipe run between a gravity tank and the system's connection point.
 *
 * <p>Like {@link SprinklerPipeSegment} without the network: no node ids, because the supply side
 * is a single path rather than a tree, and no elevation change, because the tank's height above
 * the connection is stated once on {@link GravityTankSupply} and double-counting it here would
 * inflate the available pressure.
 *
 * @param pipeMaterial  material code for {@code PipeDimensionResolver} (e.g. {@code BI})
 * @param schedule      schedule code (e.g. {@code SCH40}); may be null where the material has one
 * @param nominalSize   nominal size key as published in {@code reference/pipes/*.json}; the actual
 *                      bore is resolved from reference data, never assumed
 * @param cFactor       Hazen-Williams C for this run; must be positive
 * @param pipeLengthFt  centerline length, ft; must not be negative
 * @param fittings      fittings on this run; may be empty or null
 * @throws CalculationException if any rule above is violated
 */
public record SupplyPipeRun(
		String pipeMaterial,
		String schedule,
		String nominalSize,
		double cFactor,
		double pipeLengthFt,
		List<SegmentFitting> fittings) {

	public SupplyPipeRun {
		if (pipeMaterial == null || pipeMaterial.isBlank()) {
			throw new CalculationException("pipeMaterial is required for a supply pipe run");
		}
		if (nominalSize == null || nominalSize.isBlank()) {
			throw new CalculationException("nominalSize is required for a supply pipe run");
		}
		if (cFactor <= 0) {
			throw new CalculationException("cFactor must be positive for a supply pipe run (was " + cFactor + ")");
		}
		if (pipeLengthFt < 0) {
			throw new CalculationException(
					"pipeLengthFt must not be negative for a supply pipe run (was " + pipeLengthFt + ")");
		}
		fittings = fittings == null ? List.of() : List.copyOf(fittings);
	}

}
