package com.renzoproject.calc.core.mechanical.standpipe;

/**
 * How much of the building is protected by automatic sprinklers, which decides how the sprinkler
 * demand combines with the standpipe demand (10.6.1.3).
 */
public enum SprinklerProtection {

	/** No sprinklers: the sprinkler demand is ignored. */
	NONE,

	/** Sprinklered throughout to NFPA 13: the supply is the greater of the two demands (10.6.1.3.1). */
	FULL,

	/** Partly sprinklered: the standpipe flow is increased by an allowance (10.6.1.3.3). */
	PARTIAL

}
