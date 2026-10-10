package com.renzoproject.calc.core.mechanical.sprinkler;

/**
 * One row of {@code reference/sprinkler/sprinkler-k-factors.json}, as published.
 *
 * @param nominalK        nominal K-factor, gpm/psi^0.5 (US)
 * @param orificeNominal  nominal orifice size, or {@code null} where it isn't confirmed
 * @param confidence      {@code verified}, {@code verify} or {@code placeholder}
 */
public record SprinklerKFactorRow(
		double nominalK,
		String orificeNominal,
		String label,
		String confidence) {

}
