package com.renzoproject.calc.core.mechanical.pipe;

import com.renzoproject.calc.core.exception.CalculationException;

import javax.measure.Quantity;
import javax.measure.quantity.Temperature;

/**
 * Resolves density and dynamic viscosity for a fluid at a given temperature, for
 * {@code PipePressureLossCalculator}. Mirrors the separation already established between
 * calculators and lookup tables elsewhere in this codebase.
 */
public interface FluidPropertiesResolver {

	/**
	 * @throws CalculationException if {@code fluidKey} doesn't match published reference data,
	 *                              or {@code temperature} falls outside the reference table's
	 *                              range (never extrapolated)
	 */
	FluidProperties resolve(String fluidKey, Quantity<Temperature> temperature);

}
