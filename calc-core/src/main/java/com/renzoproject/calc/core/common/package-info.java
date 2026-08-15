/**
 * General physical-property reference data reusable across any domain, not owned by one
 * calculator's package the way {@code electrical.reference} or {@code mechanical.pipe}'s fluid
 * properties are. {@link com.renzoproject.calc.core.common.AirPropertiesResolver} is the first
 * resident -- air/combustion-gas properties (Cp, atmospheric pressure, specific gas constant)
 * needed by {@code smokecontrol} today, but equally usable by any future HVAC/ventilation
 * calculator.
 */
package com.renzoproject.calc.core.common;
