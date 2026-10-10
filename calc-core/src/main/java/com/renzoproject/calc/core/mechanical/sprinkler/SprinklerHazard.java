package com.renzoproject.calc.core.mechanical.sprinkler;

/**
 * NFPA 13 occupancy hazard classification at the granularity the density/area curves use --
 * Ordinary and Extra Hazard are split into groups, which the coarser
 * {@code mechanical.storage.HazardClassification} (hose stream/duration only) deliberately
 * does not do.
 */
public enum SprinklerHazard {
	LIGHT,
	ORDINARY_GROUP_1,
	ORDINARY_GROUP_2,
	EXTRA_GROUP_1,
	EXTRA_GROUP_2
}
