package com.renzoproject.calc.core.mechanical.pipe;

import javax.measure.Quantity;
import javax.measure.quantity.Length;

public record PipeDimension(
		String nominalSize,
		String nominalLabel,
		Quantity<Length> internalDiameter,
		Quantity<Length> outsideDiameter) {

}
