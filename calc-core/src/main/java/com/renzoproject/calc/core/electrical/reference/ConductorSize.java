package com.renzoproject.calc.core.electrical.reference;

/**
 * Canonical conductor size identity, matching a PEC reference table's size notation exactly
 * (e.g. {@code "2.0"}, {@code "3.5"}, {@code "14"}, {@code "500"}, {@code "1000"}).
 *
 * <p>Deliberately generic (no voltage-drop-specific fields) so it can be reused by any future
 * calculator that needs to key off conductor size — wire sizing, conduit fill, etc.
 *
 * @param label            size label as published in PEC tables
 * @param crossSectionMm2  nominal cross-sectional area, in square millimeters
 */
public record ConductorSize(String label, double crossSectionMm2) {

}
