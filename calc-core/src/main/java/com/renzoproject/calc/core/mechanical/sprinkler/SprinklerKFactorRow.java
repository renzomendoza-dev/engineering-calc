package com.renzoproject.calc.core.mechanical.sprinkler;

/**
 * One row of {@code reference/sprinkler/sprinkler-k-factors.json}, as published in NFPA 13's
 * sprinkler discharge characteristics table.
 *
 * @param nominalK              nominal K-factor, gpm/psi^0.5 (US)
 * @param nominalKLpmBar        the same nominal K in L/min/bar^0.5, as published
 * @param kFactorRangeMinGpm    lowest actual K a sprinkler may have and still carry this nominal
 *                              K, gpm/psi^0.5
 * @param kFactorRangeMaxGpm    highest actual K for this nominal K
 * @param percentOfK56Discharge discharge as a percentage of a K-5.6 sprinkler's, as published
 * @param threadType            published thread, e.g. {@code "1/2 in. (15 mm) NPT"}. This is a
 *                              thread size, not an orifice size -- the standard's table does not
 *                              publish orifice diameters
 * @param confidence            {@code verified}, {@code verify} or {@code placeholder}
 */
public record SprinklerKFactorRow(
		double nominalK,
		double nominalKLpmBar,
		double kFactorRangeMinGpm,
		double kFactorRangeMaxGpm,
		double percentOfK56Discharge,
		String threadType,
		String label,
		String confidence) {

}
