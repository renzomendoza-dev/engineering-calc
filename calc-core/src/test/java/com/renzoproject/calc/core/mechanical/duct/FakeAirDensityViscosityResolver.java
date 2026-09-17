package com.renzoproject.calc.core.mechanical.duct;

import com.renzoproject.calc.core.mechanical.pipe.FluidProperties;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Temperature;

class FakeAirDensityViscosityResolver implements AirDensityViscosityResolver {

	private final FluidProperties properties;

	FakeAirDensityViscosityResolver(FluidProperties properties) {
		this.properties = properties;
	}

	@Override
	public FluidProperties resolve(Quantity<Temperature> temperature, Quantity<Length> altitude) {
		return properties;
	}

}
