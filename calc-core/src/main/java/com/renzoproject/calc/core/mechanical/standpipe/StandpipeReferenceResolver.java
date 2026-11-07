package com.renzoproject.calc.core.mechanical.standpipe;

import com.renzoproject.calc.core.exception.CalculationException;
import java.util.List;

/**
 * Resolves the NFPA 14 equivalent length chart and Hazen-Williams C table.
 */
public interface StandpipeReferenceResolver {

	/**
	 * @throws CalculationException if {@code fittingType} isn't published
	 */
	StandpipeFittingRow resolveFitting(String fittingType);

	/** The C the published lengths are based on (120): they are rescaled for any other C. */
	double cFactorBasis();

	List<StandpipeFittingRow> allFittings();

	List<StandpipeCFactorRow> allCFactors();

	/** Table A.10.2.6: the hose and nozzle combinations a form can offer for a connection's design residual pressure. */
	List<StandpipeHoseNozzleRow> allHoseNozzleCombinations();

	/** The Annex's own caution about using the table, for display next to it. */
	String hoseNozzleNote();

	/** Edition and confidence note of the file, for display next to the tables. */
	String edition();

	String note();

}
