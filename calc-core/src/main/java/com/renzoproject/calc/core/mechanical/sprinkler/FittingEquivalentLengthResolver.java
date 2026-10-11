package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;

import java.util.List;

/**
 * Resolves fitting equivalent pipe lengths (NFPA 13 Table 27.2.3.1.1).
 */
public interface FittingEquivalentLengthResolver {

	/**
	 * @throws CalculationException if {@code fittingType} isn't published
	 */
	FittingEquivalentLengthRow resolve(String fittingType);

	/**
	 * The C factor the published lengths are based on. NFPA 13 requires multiplying them by
	 * {@code (basis/C)^1.85} for pipe with a different C factor; see {@code reference/sprinkler/README.md}.
	 */
	double cFactorBasis();

	/** All rows as published, for a reference table. */
	List<FittingEquivalentLengthRow> allEntries();

}
