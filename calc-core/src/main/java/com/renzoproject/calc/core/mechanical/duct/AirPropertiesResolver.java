package com.renzoproject.calc.core.mechanical.duct;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.FluidProperties;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Temperature;

/**
 * Resolves air density/viscosity by temperature and altitude, for
 * {@link DuctSizingCalculator}. Reuses {@code mechanical.pipe}'s existing {@link FluidProperties}
 * record rather than a duplicate type -- air is just another fluid as far as the friction-loss
 * math is concerned.
 *
 * <p>Distinct from {@code com.renzoproject.calc.core.common.AirPropertiesResolver} (used by
 * {@code smokecontrol}, which resolves fixed Cp/Patm/R/g constants from a JSON reference file):
 * this interface resolves temperature/altitude-dependent density and viscosity analytically, a
 * different shape for a different purpose. Same name, different package, deliberately not
 * unified -- {@code smokecontrol}'s resolver returns constants for its own plume formulas, this
 * one returns a computed {@link FluidProperties} for a friction-loss calculation, and the two
 * packages have no reason to share a resolver contract.
 */
public interface AirPropertiesResolver {

	/**
	 * @throws CalculationException if either argument is missing, or if the resolved conditions
	 *                               are outside what the resolver considers physically reasonable
	 */
	FluidProperties resolve(Quantity<Temperature> temperature, Quantity<Length> altitude);

}
