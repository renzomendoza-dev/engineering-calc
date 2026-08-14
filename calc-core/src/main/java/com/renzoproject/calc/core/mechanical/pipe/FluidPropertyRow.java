package com.renzoproject.calc.core.mechanical.pipe;

/**
 * One row of published fluid property data, shaped per
 * {@code reference/fluids/fluids-README.md}. Package-private — internal to
 * {@link JsonFluidPropertiesResolver}'s JSON parsing.
 */
record FluidPropertyRow(double temperatureC, double density, double dynamicViscosity) {

}
