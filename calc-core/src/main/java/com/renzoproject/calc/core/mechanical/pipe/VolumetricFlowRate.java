package com.renzoproject.calc.core.mechanical.pipe;

import javax.measure.Quantity;

/**
 * Volume flow rate (e.g. m3/s, L/min). Not part of the core JSR-385 quantity set the way
 * {@code Length}/{@code Speed}/{@code Volume} are, so it's defined here following the standard
 * "custom quantity" extension pattern documented by the Units of Measurement API: a marker
 * interface extending {@code Quantity<Self>}, paired with a derived {@code Unit} built via
 * {@code someUnit.asType(CustomQuantity.class)} (see {@link PipeUnits}). This avoids pulling in
 * an extra units library (e.g. uom-systems) just for one derived quantity.
 */
public interface VolumetricFlowRate extends Quantity<VolumetricFlowRate> {

}
