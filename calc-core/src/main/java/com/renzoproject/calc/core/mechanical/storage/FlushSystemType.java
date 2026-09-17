package com.renzoproject.calc.core.mechanical.storage;

/**
 * Whether a building's water closets are predominantly flush-tank or flush-valve, for
 * {@link DemandBasis#FIXTURE_UNIT} mode. Selects which peak-demand column of the WSFU table
 * applies, since flush valves draw far more at low fixture-unit counts.
 */
public enum FlushSystemType {
	FLUSH_TANK,
	FLUSH_VALVE
}
