package com.renzoproject.calc.core.electrical.reference;

/**
 * Resistance and reactance for a conductor, in ohms per meter, as resolved by
 * {@link ConductorPropertiesResolver} for a given circuit type, size, and material
 * combination. Feeds directly into a calculator's input (e.g.
 * {@code VoltageDropInput.resistanceOhmsPerMeter()} / {@code reactanceOhmsPerMeter()}).
 */
public record ConductorProperties(double resistanceOhmsPerMeter, double reactanceOhmsPerMeter) {

}
