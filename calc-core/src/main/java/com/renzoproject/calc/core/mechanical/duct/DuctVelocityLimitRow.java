package com.renzoproject.calc.core.mechanical.duct;

/**
 * One row of {@code reference/duct/duct-velocity-limits.json}'s {@code limits}. Public (not
 * package-private) so {@link DuctVelocityLimitResolver#allEntries()} can return it for display
 * purposes, same reasoning as {@code WsfuDemandRow} in {@code mechanical.storage}.
 */
public record DuctVelocityLimitRow(String ductLocation, String label, int ncRcRating, double maxVelocityRectangular, double maxVelocityRound) {

}
