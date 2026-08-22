package com.renzoproject.calc.core.mechanical.duct;

import com.renzoproject.calc.core.exception.CalculationException;

import java.util.List;

/**
 * Resolves absolute roughness (mm) by duct material, for {@link DuctSizingCalculator}'s friction
 * factor calculation.
 */
public interface DuctRoughnessResolver {

	/**
	 * @throws CalculationException if {@code material} isn't found
	 */
	double resolveAbsoluteRoughnessMm(String material);

	/**
	 * All raw rows, as published. Intended for populating a frontend material dropdown and
	 * displaying the table itself, as opposed to {@link #resolveAbsoluteRoughnessMm} which
	 * resolves a single material.
	 */
	List<DuctRoughnessRow> allEntries();

}
