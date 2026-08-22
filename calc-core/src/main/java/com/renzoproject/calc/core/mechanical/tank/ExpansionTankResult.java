package com.renzoproject.calc.core.mechanical.tank;

import javax.measure.Quantity;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Volume;

/**
 * Result of {@link ExpansionTankCalculator}.
 *
 * @param derivedSystemVolume         estimated total system water volume (primary vessel + piping),
 *                                    scaled by {@code additionalVolumeFactor}
 * @param expansionVolume             volume increase as the system heats from cold to hot
 * @param requiredTankVolume          minimum expansion tank volume needed to absorb
 *                                    {@code expansionVolume} within the fill/max-operating
 *                                    pressure charge
 * @param coldWaterDensityKgM3        density used at {@code coldWaterTemperature}
 * @param hotWaterDensityKgM3         density used at {@code hotWaterTemperature}
 * @param fillPressureAbsolute        gauge fill pressure converted to absolute at altitude
 * @param maxOperatingPressureAbsolute gauge max operating pressure converted to absolute at altitude
 */
public record ExpansionTankResult(
		Quantity<Volume> derivedSystemVolume,
		Quantity<Volume> expansionVolume,
		Quantity<Volume> requiredTankVolume,
		double coldWaterDensityKgM3,
		double hotWaterDensityKgM3,
		Quantity<Pressure> fillPressureAbsolute,
		Quantity<Pressure> maxOperatingPressureAbsolute) {

}
