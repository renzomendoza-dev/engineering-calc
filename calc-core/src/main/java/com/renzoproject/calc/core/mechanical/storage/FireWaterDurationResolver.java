package com.renzoproject.calc.core.mechanical.storage;

import com.renzoproject.calc.core.exception.CalculationException;

import java.util.List;

/**
 * Resolves the required water supply duration range for an NFPA 13 hazard classification, for
 * {@link FireWaterStorageCalculator}. This is a pre-step that runs before any calculation -- it
 * does no math of its own, mirroring {@code ConductorPropertiesResolver}'s separation of
 * reference-data lookup from calculation.
 */
public interface FireWaterDurationResolver {

	/**
	 * @throws CalculationException if {@code classification} is {@code null} or unrecognized
	 */
	DurationRange resolve(HazardClassification classification);

	/**
	 * All classifications' resolved ranges, as published. Intended for displaying the table
	 * itself (e.g. a frontend reference table), as opposed to {@link #resolve} which resolves
	 * a single classification.
	 */
	List<FireWaterDurationEntry> allEntries();

}
