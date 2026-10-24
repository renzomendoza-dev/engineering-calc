package com.renzoproject.calc.core.electrical.generatorset;

/**
 * Which Chapter 7 article the standby source falls under. Fixes the capacity rule that applies
 * and the start-time, fuel and equipment requirements listed with the result.
 */
public enum StandbyClass {
	EMERGENCY,
	LEGALLY_REQUIRED,
	OPTIONAL
}
