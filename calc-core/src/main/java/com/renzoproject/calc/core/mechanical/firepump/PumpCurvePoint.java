package com.renzoproject.calc.core.mechanical.firepump;

import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;

import javax.measure.Quantity;
import javax.measure.quantity.Pressure;

public record PumpCurvePoint(Quantity<VolumetricFlowRate> flow, Quantity<Pressure> pressure) {

}
