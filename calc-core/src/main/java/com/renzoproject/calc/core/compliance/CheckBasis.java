package com.renzoproject.calc.core.compliance;

/**
 * Where a {@link ComplianceCheck} comes from, so a report can label what is a requirement of
 * the standard and what is not. Only calculators that mix the two set it (natural venting, where
 * the standard is NFPA 204); the others leave it {@code null}.
 */
public enum CheckBasis {

	/** A clause of the standard's body (shall requirement or equation). */
	NFPA_204,

	/** Informative annex content or the designer's own criterion; not a requirement of the standard. */
	SUPPLEMENTARY

}
