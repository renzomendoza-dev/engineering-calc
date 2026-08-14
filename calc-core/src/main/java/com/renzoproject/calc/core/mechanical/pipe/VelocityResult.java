package com.renzoproject.calc.core.mechanical.pipe;

import javax.measure.Quantity;
import javax.measure.quantity.Speed;

public record VelocityResult(Quantity<Speed> velocity) implements PipeSizingResult {

}
