package com.renzoproject.calc.core.mechanical.firepump;

import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;

import javax.measure.Quantity;

public record StandardPumpRating(Quantity<VolumetricFlowRate> standardFlow) {

}
