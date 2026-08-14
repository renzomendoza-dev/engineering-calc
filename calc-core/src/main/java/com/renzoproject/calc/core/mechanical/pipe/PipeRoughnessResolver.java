package com.renzoproject.calc.core.mechanical.pipe;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Resolves a pipe material's absolute roughness, for friction-factor calculations
 * ({@code PipePressureLossCalculator}). Reads the same {@code reference/pipes/{material}.json}
 * files {@link PipeDimensionResolver} already parses — see {@code JsonPipeDimensionResolver},
 * which implements both interfaces rather than re-parsing the file a second time.
 */
public interface PipeRoughnessResolver {

	/**
	 * @throws CalculationException if material doesn't match published reference data, or has
	 *                              no {@code hydraulics} block
	 */
	double resolveAbsoluteRoughnessMm(String material);

}
