package com.renzoproject.calc.core.electrical.reference;

/**
 * AC resistance and reactance for a conductor, in ohms per meter, as resolved by
 * {@link ConductorImpedanceTable}.
 */
public record ConductorImpedance(double resistanceOhmsPerMeter, double reactanceOhmsPerMeter) {

}
