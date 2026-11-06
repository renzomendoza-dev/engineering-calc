package com.renzoproject.calc.core.mechanical.standpipe;

/**
 * Size of a hose connection, which fixes its minimum residual pressure (7.8.1), its maximum flow
 * (7.10.3) and its pressure limit (7.2.3).
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

	/** Residual pressure required at the outlet of the hydraulically most remote connection of this size, psi (7.8.1). */
	public double minimumResidualPsi() {
		return minimumResidualPsi;
	}

	/** The most a single connection of this size is required to deliver, gpm (7.10.3). */
	public double maximumFlowGpm() {
		return maximumFlowGpm;
	}

}
