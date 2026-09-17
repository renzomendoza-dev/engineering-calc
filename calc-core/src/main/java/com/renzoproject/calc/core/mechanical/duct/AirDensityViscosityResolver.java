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
 * <p>Not to be confused with {@code common.AirPropertiesResolver}, which smoke control uses to look
 * up fixed Cp/Patm/R/g constants from a JSON reference file. This interface computes
 * temperature- and altitude-dependent density and viscosity for a friction-loss calculation. The
 * two answer different questions and deliberately don't share a contract; they used to share a
 * simple name too, which is why this one is named for what it returns.
 */
public interface AirDensityViscosityResolver {

	/**
	 * @throws CalculationException if either argument is missing, or if the resolved conditions
	 *                               are outside what the resolver considers physically reasonable
	 */
	FluidProperties resolve(Quantity<Temperature> temperature, Quantity<Length> altitude);

}
