package com.renzoproject.calc.core.mechanical.storage;

/**
 * One row of {@code reference/storage/lpcd-consumption.json}'s {@code occupancyTypes}. Public
 * (not package-private) so {@link PerCapitaConsumptionResolver#allEntries()} can return it for
 * display purposes, same reasoning as {@code AmpacityEntry} in {@code electrical.reference}.
 */
public record OccupancyTypeRow(String type, String label, double lpcd) {

}
