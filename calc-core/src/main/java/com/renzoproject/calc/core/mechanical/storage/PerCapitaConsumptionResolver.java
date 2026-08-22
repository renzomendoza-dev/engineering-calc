package com.renzoproject.calc.core.mechanical.storage;

import com.renzoproject.calc.core.exception.CalculationException;

import java.util.List;

/**
 * Resolves per-capita daily water consumption (LPCD -- litres per capita per day) by occupancy
 * type, for {@link DomesticWaterStorageCalculator}'s {@link DemandBasis#OCCUPANT_LOAD} mode.
 *
 * <p><b>Confidence: placeholder.</b> {@code reference/storage/lpcd-consumption.json} -- the only
 * shipped implementation of this interface ({@link JsonPerCapitaConsumptionResolver}) -- is
 * generic, internationally-cited rule-of-thumb consumption figures, NOT sourced from a confirmed
 * Philippine code (see that file's entry in {@code reference/storage/README.md} for what was
 * checked and came up empty). Treat {@code OCCUPANT_LOAD} results as a rough sizing estimate
 * pending a verified source -- do not treat them as more authoritative than the
 * {@link FixtureUnitDemandResolver} path, which is verified against two independently-published
 * tables.
 */
public interface PerCapitaConsumptionResolver {

	/**
	 * @throws CalculationException if {@code occupancyType} isn't found
	 */
	double resolveLpcd(String occupancyType);

	/**
	 * All raw rows, as published. Intended for displaying the table itself (e.g. a frontend
	 * reference table), as opposed to {@link #resolveLpcd} which resolves a single occupancy
	 * type.
	 */
	List<OccupancyTypeRow> allEntries();

}
