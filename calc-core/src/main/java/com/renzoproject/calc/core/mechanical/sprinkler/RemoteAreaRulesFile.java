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
		String remoteAreaLengthConfidence,
		String remoteAreaLengthSource,
		double minimumOperatingPressurePsi,
		String minimumOperatingPressureNote,
		String minimumOperatingPressureConfidence,
		String minimumOperatingPressureSource,
		double maximumOperatingPressurePsi,
		String maximumOperatingPressureConfidence,
		String maximumOperatingPressureSource,
		String maximumOperatingPressureNote) {

}
