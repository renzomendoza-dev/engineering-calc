package com.renzoproject.calc.core.mechanical.standpipe;

/**
 * Size of a fire department connection inlet and the flow it is assumed to carry: one 2.5 in. inlet
 * per 250 gpm (10.7.3), 500 gpm for a 4 in. inlet (10.7.3.1.1) and 750 gpm for a 5 in. inlet
 * (10.7.3.1.2).
 */
public enum InletSize {

	TWO_AND_HALF_INCH("2.5 in.", 250.0, "10.7.3"),
	FOUR_INCH("4 in.", 500.0, "10.7.3.1.1"),
	FIVE_INCH("5 in.", 750.0, "10.7.3.1.2");

	private final String label;
	private final double flowPerInletGpm;
	private final String clause;

	InletSize(String label, double flowPerInletGpm, String clause) {
		this.label = label;
		this.flowPerInletGpm = flowPerInletGpm;
		this.clause = clause;
	}

	/** Size as written in the results, for example {@code "4 in."}. */
	public String label() {
		return label;
	}

	/** The flow assumed for one inlet of this size, gpm. */
	public double flowPerInletGpm() {
		return flowPerInletGpm;
	}

	/** The clause that sets the flow for this size. */
	public String clause() {
		return clause;
	}

	/** Whether this is a large diameter inlet, whose hose pressure limit must be considered (10.7.3.1.3). */
	public boolean largeDiameter() {
		return this != TWO_AND_HALF_INCH;
	}

}
