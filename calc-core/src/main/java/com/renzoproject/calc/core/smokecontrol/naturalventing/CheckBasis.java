package com.renzoproject.calc.core.smokecontrol.naturalventing;

/**
 * Where a {@link VentingCheck} comes from, so a report can label what is a requirement of
 * NFPA 204 and what is not.
 */
public enum CheckBasis {

	/** A clause of the standard's body (shall requirement or equation). */
	NFPA_204,

	/** Informative annex content or the designer's own criterion; not a requirement of NFPA 204. */
	SUPPLEMENTARY

}
