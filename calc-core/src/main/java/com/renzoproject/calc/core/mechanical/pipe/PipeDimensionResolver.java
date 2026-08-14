package com.renzoproject.calc.core.mechanical.pipe;

import com.renzoproject.calc.core.exception.CalculationException;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import java.util.List;

/**
 * Resolves nominal pipe sizes (or a calculated raw diameter) to real dimensions from reference
 * data. Mirrors the separation already established between calculators and lookup tables in the
 * electrical package (e.g. {@code ConductorPropertiesResolver}) — no calculator in this codebase
 * does its own reference-data lookups.
 */
public interface PipeDimensionResolver {

	/**
	 * @throws CalculationException if material/schedule/nominalLabel doesn't match published
	 *                              reference data
	 */
	PipeDimension resolve(String material, String schedule, String nominalLabel);

	/**
	 * Selects the smallest published size whose internal diameter is {@code >=}
	 * {@code calculatedMinDiameter} — rounds up, never down.
	 *
	 * @throws CalculationException if material/schedule doesn't match published reference data,
	 *                              or if no published size is large enough (never silently
	 *                              returns the largest available size instead)
	 */
	PipeDimension resolveNextStandardSize(Quantity<Length> calculatedMinDiameter, String material, String schedule);

	/**
	 * Lists every published material with its full schedule/size breakdown, for populating
	 * frontend material/schedule/nominal-size dropdowns. Unlike {@link #resolve} and
	 * {@link #resolveNextStandardSize}, callers here want the whole reference catalog at
	 * once, not a single matched dimension.
	 */
	List<PipeMaterialReference> listAllMaterials();

}
