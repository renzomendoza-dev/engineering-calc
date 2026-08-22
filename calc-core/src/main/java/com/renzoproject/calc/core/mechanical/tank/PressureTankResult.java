package com.renzoproject.calc.core.mechanical.tank;

import javax.measure.Quantity;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Volume;

/**
 * Result of {@link PressureTankCalculator}.
 *
 * @param drawdownVolume        {@code drawdownFlowRate * minRunTimeMinutes}, unit-aligned
 * @param requiredTankVolume    minimum tank volume needed to deliver {@code drawdownVolume}
 *                              without the pressure crossing outside the low/high band
 * @param lowPressureAbsolute   gauge low pressure converted to absolute at altitude
 * @param highPressureAbsolute  gauge high pressure converted to absolute at altitude
 */
public record PressureTankResult(
		Quantity<Volume> drawdownVolume,
		Quantity<Volume> requiredTankVolume,
		Quantity<Pressure> lowPressureAbsolute,
		Quantity<Pressure> highPressureAbsolute) {

}
