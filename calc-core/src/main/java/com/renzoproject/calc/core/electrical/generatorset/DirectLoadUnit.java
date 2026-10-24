package com.renzoproject.calc.core.electrical.generatorset;

/**
 * The unit a total load is entered in. {@code KW} needs a power factor to become kVA; {@code AMPS}
 * is read at the system voltage.
 */
public enum DirectLoadUnit {
	KVA,
	KW,
	AMPS
}
