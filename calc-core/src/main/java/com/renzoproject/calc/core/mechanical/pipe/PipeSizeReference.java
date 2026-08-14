package com.renzoproject.calc.core.mechanical.pipe;

/**
 * One published pipe size, for populating a frontend nominal-size dropdown.
 *
 * @param nominalSize     machine-friendly key — pass this (not {@code nominalLabel}) as
 *                        {@code NominalSize.nominalLabel()} / the request's
 *                        {@code nominalLabel} field when resolving a dimension. Same
 *                        naming gotcha documented on {@link NominalSize}.
 * @param nominalLabel    human-readable, for display.
 * @param internalDiameterMm the value actually used in {@code V = Q/A} math.
 */
public record PipeSizeReference(String nominalSize, String nominalLabel, double internalDiameterMm) {

}
