package com.renzoproject.calc.core.electrical.voltagedrop;

/**
 * Distribution topology of the circuit being evaluated for voltage drop.
 * Determines whether reactance/power factor apply and which phase factor
 * ({@code 2.0} vs {@code sqrt(3)}) is used in the drop formula.
 */
public enum CircuitType {

	/** Direct current. Reactance is not applicable and must be {@code 0}. */
	DC,

	/** Single-phase alternating current (line + neutral/return conductor). */
	SINGLE_PHASE_AC,

	/** Three-phase alternating current. */
	THREE_PHASE_AC
}
