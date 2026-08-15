package com.renzoproject.calc.core.mechanical.pump;

import com.renzoproject.calc.core.mechanical.pipe.FluidProperties;
import com.renzoproject.calc.core.mechanical.pipe.FluidPropertiesResolver;

import javax.measure.Quantity;
import javax.measure.quantity.Temperature;

/**
 * Minimal in-memory {@link FluidPropertiesResolver} — ignores {@code fluidKey}/{@code temperature}
 * and always returns the same fixed properties, so {@link PumpTDHCalculatorTest} isn't coupled
 * to real reference data values.
 */
class FakeFluidPropertiesResolver implements FluidPropertiesResolver {

	private final FluidProperties properties;

	FakeFluidPropertiesResolver(double densityKgM3, double dynamicViscosityPaS) {
		this.properties = new FluidProperties(densityKgM3, dynamicViscosityPaS);
	}

	@Override
	public FluidProperties resolve(String fluidKey, Quantity<Temperature> temperature) {
		return properties;
	}

}
