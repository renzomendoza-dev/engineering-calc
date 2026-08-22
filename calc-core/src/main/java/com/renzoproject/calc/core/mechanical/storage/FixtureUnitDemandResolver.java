package com.renzoproject.calc.core.mechanical.storage;

import com.renzoproject.calc.core.exception.CalculationException;

import java.util.List;

/**
 * Resolves peak demand (GPM) from total water supply fixture units (WSFU), for
 * {@link DomesticWaterStorageCalculator}'s {@link DemandBasis#FIXTURE_UNIT} mode.
 *
 * <p><b>Confidence: verified</b> -- {@code reference/storage/wsfu-demand.json}, the only shipped
 * implementation ({@link JsonFixtureUnitDemandResolver}), is cross-validated against a real
 * worked example from the Revised National Plumbing Code of the Philippines; see that file's
 * entry in {@code reference/storage/README.md}.
 */
public interface FixtureUnitDemandResolver {

	/**
	 * Linearly interpolates between bracketing {@code wsfu} rows for the requested
	 * {@code systemType}.
	 *
	 * @throws CalculationException if {@code totalWsfu} exceeds the table's upper bound (10,000
	 *                               WSFU -- never extrapolate), if {@code systemType} is
	 *                               {@link SystemType#FLUSH_VALVE} and {@code totalWsfu} is below
	 *                               5 (no published flush-valve data there), or if
	 *                               {@code totalWsfu} otherwise falls below the table's lowest
	 *                               known value for the requested system type
	 */
	double resolveGpm(double totalWsfu, SystemType systemType);

	/**
	 * All raw rows, as published, ascending by WSFU. Intended for displaying the table itself
	 * (e.g. a frontend reference table), as opposed to {@link #resolveGpm} which resolves (and
	 * interpolates) a single WSFU value.
	 */
	List<WsfuDemandRow> allEntries();

}
