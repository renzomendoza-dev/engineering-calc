package com.renzoproject.calc.core.electrical.generatorset;

/**
 * How load reaches an optional standby source, which decides the capacity rule in PEC 7.2.1.4(B).
 */
public enum OptionalStandbyTransfer {
	MANUAL,
	AUTOMATIC_FULL_LOAD,
	AUTOMATIC_LOAD_MANAGEMENT
}
