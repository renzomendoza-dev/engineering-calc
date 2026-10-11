package com.renzoproject.calc.core.mechanical.sprinkler;

/**
 * Top-level shape of {@code reference/sprinkler/remote-area-rules.json}. Package-private --
 * internal to {@link JsonRemoteAreaRulesResolver}'s JSON parsing.
 */
record RemoteAreaRulesFile(
		String standard,
		String edition,
		String confidence,
		String note,
		double remoteAreaLengthFactor,
		String remoteAreaLengthNote,
		double minimumOperatingPressurePsi,
		String minimumOperatingPressureNote) {

}
