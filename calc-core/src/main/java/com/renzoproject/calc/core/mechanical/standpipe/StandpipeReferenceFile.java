package com.renzoproject.calc.core.mechanical.standpipe;

import java.util.List;

/**
 * Top-level shape of {@code reference/standpipe/standpipe-reference.json}.
 * Package-private: internal to {@link JsonStandpipeReferenceResolver}.
 */
record StandpipeReferenceFile(
		String standard,
		String edition,
		String confidence,
		String note,
		double cFactorBasis,
		String cFactorBasisNote,
		List<StandpipeFittingRow> fittings,
		List<StandpipeCFactorRow> cFactors) {

}
