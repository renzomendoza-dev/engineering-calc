package com.renzoproject.calc.core.common;

/**
 * General air/combustion-gas physical properties, from {@code reference/common/air-properties.json}.
 *
 * @param specificHeatKjPerKgK         Cp, kJ/kg-K
 * @param atmosphericPressurePa        Patm, Pa
 * @param specificGasConstantJPerKgK   R, J/kg-K
 * @param gravitationalAccelerationMPerS2 g, m/s2 -- standard gravity
 */
public record AirProperties(
		double specificHeatKjPerKgK,
		double atmosphericPressurePa,
		double specificGasConstantJPerKgK,
		double gravitationalAccelerationMPerS2) {

}
