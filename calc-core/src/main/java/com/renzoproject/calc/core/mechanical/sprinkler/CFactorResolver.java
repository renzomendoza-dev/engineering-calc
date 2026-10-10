package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;

import java.util.List;

/**
 * Resolves the reference Hazen-Williams C factor for a pipe type in a given service.
 *
 * <p>Not used by {@link SprinklerHydraulicCalculator}: C is supplied per pipe segment, since a
 * system can mix materials (steel branch lines on a ductile iron underground main) and a designer
 * may deliberately use a more conservative value than the table's. This resolver publishes the
 * reference set for a caller or a form to default from.
 */
public interface CFactorResolver {

	/**
	 * Prefers a row matching {@code systemType} exactly, falling back to the row published for
	 * {@link CFactorRow#ANY_SYSTEM_TYPE}.
	 *
	 * @throws CalculationException if no published row matches
	 */
	CFactorRow resolve(String pipeType, SprinklerSystemType systemType);

	/** All rows as published, for a reference table or a form's default. */
	List<CFactorRow> allEntries();

}
