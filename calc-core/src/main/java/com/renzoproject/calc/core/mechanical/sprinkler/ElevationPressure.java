package com.renzoproject.calc.core.mechanical.sprinkler;

/**
 * Static pressure change over a change in elevation: {@code 0.433 psi per foot of rise}.
 *
 * <p><b>Signed:</b> a rise (positive feet, water going up) costs pressure and returns a positive
 * psi value to be <i>added</i> to the required pressure; a drop returns a negative value, which
 * gives pressure back. 0.433 psi/ft is the conventional NFPA 13 value for water (1 ft of head at
 * about 60 degF).
 */
public final class ElevationPressure {

	/** psi per foot of water column, the conventional sprinkler-hydraulics value. */
	public static final double PSI_PER_FOOT = 0.433;

	private ElevationPressure() {
	}

	/**
	 * @param riseFeet elevation gain in feet, negative for a drop
	 * @return psi to add to the required pressure (negative when the pipe drops)
	 */
	public static double psiForRise(double riseFeet) {
		return PSI_PER_FOOT * riseFeet;
	}

}
