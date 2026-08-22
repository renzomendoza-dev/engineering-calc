package com.renzoproject.calc.core.mechanical.storage;

/**
 * One row of {@code reference/storage/wsfu-demand.json}'s {@code demandTable}. Public (not
 * package-private) so {@link FixtureUnitDemandResolver#allEntries()} can return it for display
 * purposes, same reasoning as {@code AmpacityEntry} in {@code electrical.reference}.
 *
 * @param gpmFlushValves {@code null} below WSFU=5 (not listed in the source table) -- see
 *                       {@code reference/storage/README.md}
 */
public record WsfuDemandRow(double wsfu, double gpmFlushTanks, Double gpmFlushValves) {

}
