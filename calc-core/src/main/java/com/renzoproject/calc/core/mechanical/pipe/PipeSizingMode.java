package com.renzoproject.calc.core.mechanical.pipe;

public enum PipeSizingMode {
	/** Analysis: given flow rate and a known diameter, find velocity. */
	VELOCITY_FROM_DIAMETER,
	/** Design: given flow rate and a target velocity, find the minimum standard pipe size. */
	DIAMETER_FROM_VELOCITY
}
