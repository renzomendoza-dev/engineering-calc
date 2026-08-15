package com.renzoproject.calc.core.smokecontrol;

/**
 * Smoke-control-specific default assumptions, from {@code reference/smoke-control/defaults.json}.
 *
 * @param fractionConvectiveHeatInSmokeLayer Ks, dimensionless -- default assumption for an
 *                                            unobstructed axisymmetric plume with no significant
 *                                            heat loss to boundaries before entering the smoke
 *                                            layer
 * @param convectiveFraction                 chi, dimensionless -- typical default for flaming
 *                                            combustion
 */
public record SmokeControlDefaults(double fractionConvectiveHeatInSmokeLayer, double convectiveFraction) {

}
