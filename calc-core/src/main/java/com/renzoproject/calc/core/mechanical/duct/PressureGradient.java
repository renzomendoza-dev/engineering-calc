package com.renzoproject.calc.core.mechanical.duct;

import javax.measure.Quantity;

/**
 * Pressure change per unit length (e.g. Pa/m), such as a duct's friction rate. Not part of the
 * core JSR-385 quantity set, so it's defined with the same custom-quantity pattern as
 * {@code mechanical.pipe.VolumetricFlowRate}: a marker interface paired with a derived unit
 * ({@link DuctUnits#PASCAL_PER_METRE}).
 *
 * <p>This exists so a friction rate can't be mistaken for a pressure. Stored as a
 * {@code Quantity<Pressure>}, a Pa/m value would convert "successfully" to kPa or psi and produce
 * a meaningless number; typed as a gradient, converting it to a pressure unit doesn't compile.
 */
public interface PressureGradient extends Quantity<PressureGradient> {

}
