package com.renzoproject.calc.core.mechanical.duct;

/**
 * One row of {@code reference/duct/duct-roughness.json}'s {@code materials}. Public (not
 * package-private) so {@link DuctRoughnessResolver#allEntries()} can return it for display
 * purposes, same reasoning as {@code WsfuDemandRow} in {@code mechanical.storage}.
 */
public record DuctRoughnessRow(String material, String label, double absoluteRoughnessMm, String sourceNote) {

}
