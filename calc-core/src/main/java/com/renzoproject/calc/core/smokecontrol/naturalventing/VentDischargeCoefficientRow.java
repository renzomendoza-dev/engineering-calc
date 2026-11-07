package com.renzoproject.calc.core.smokecontrol.naturalventing;

/**
 * One default discharge coefficient for a vent or inlet type, as NFPA 204 Table 9.2.4.2 prints it
 * (or, for the row from the explanatory text, A.9.2.4.2). A default a form can offer: the
 * manufacturer's value is used when there is one (9.2.4.2).
 *
 * @param key                  stable identifier of the type
 * @param label                the type as written in the standard
 * @param dischargeCoefficient the coefficient, applicable to a vent (Cd,v) and to an inlet (Cd,i)
 * @param clause               where it is printed, for display
 * @param confidence           {@code verified} or {@code verify}
 */
public record VentDischargeCoefficientRow(
		String key,
		String label,
		double dischargeCoefficient,
		String clause,
		String confidence) {

}
