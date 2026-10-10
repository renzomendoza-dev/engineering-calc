package com.renzoproject.calc.core.mechanical.sprinkler;

/**
 * One sampled point on the water supply curve, for plotting the supply against the demand.
 *
 * @param flowGpm             flow
 * @param availablePressurePsi pressure the supply delivers at that flow
 */
public record SupplyCurvePoint(double flowGpm, double availablePressurePsi) {

}
