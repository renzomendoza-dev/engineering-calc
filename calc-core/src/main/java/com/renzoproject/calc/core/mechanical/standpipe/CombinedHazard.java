package com.renzoproject.calc.core.mechanical.standpipe;

/**
 * Hazard of a partly sprinklered building, which caps the sprinkler allowance added to the standpipe
 * flow: 150 gpm for light, 500 gpm for ordinary (7.10.1.3.2).
 */
public enum CombinedHazard {

	LIGHT(150.0),
	ORDINARY(500.0);

	private final double allowanceCapGpm;

	CombinedHazard(double allowanceCapGpm) {
		this.allowanceCapGpm = allowanceCapGpm;
	}

	public double allowanceCapGpm() {
		return allowanceCapGpm;
	}

}
