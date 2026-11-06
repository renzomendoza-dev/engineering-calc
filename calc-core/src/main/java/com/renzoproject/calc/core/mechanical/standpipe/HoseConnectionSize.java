package com.renzoproject.calc.core.mechanical.standpipe;

/**
 * Size of a hose connection, which fixes its minimum residual pressure (10.2.6.1), its maximum flow
 * (10.6.3) and its pressure limit (10.2.4).
 */
public enum HoseConnectionSize {

	ONE_AND_HALF_INCH(65.0, 100.0),
	TWO_AND_HALF_INCH(100.0, 250.0);

	private final double minimumResidualPsi;
	private final double maximumFlowGpm;

	HoseConnectionSize(double minimumResidualPsi, double maximumFlowGpm) {
		this.minimumResidualPsi = minimumResidualPsi;
		this.maximumFlowGpm = maximumFlowGpm;
	}

	/** Residual pressure required at the outlet of the hydraulically most remote connection of this size, psi (10.2.6.1). */
	public double minimumResidualPsi() {
		return minimumResidualPsi;
	}

	/** The most a single connection of this size is required to deliver, gpm (10.6.3). */
	public double maximumFlowGpm() {
		return maximumFlowGpm;
	}

}
