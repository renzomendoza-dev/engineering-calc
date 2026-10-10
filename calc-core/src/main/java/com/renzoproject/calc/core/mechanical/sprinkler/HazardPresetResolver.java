package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;

import java.util.List;

/**
 * Resolves NFPA 13 design presets (density, area, hose stream, duration, coverage) by hazard.
 *
 * <p>Not used by {@link SprinklerHydraulicCalculator}: density and area are calculation inputs,
 * because the density/area curve offers a range of valid design points and choosing one is the
 * engineer's decision. This resolver publishes the usual starting points so a caller or a form can
 * offer them.
 */
public interface HazardPresetResolver {

	/**
	 * @throws CalculationException if the hazard has no published preset
	 */
	HazardPresetRow resolve(SprinklerHazard hazard);

	/** All rows as published, for populating a hazard dropdown or reference table. */
	List<HazardPresetRow> allEntries();

}
