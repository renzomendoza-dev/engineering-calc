package com.renzoproject.calc.core.mechanical.sprinkler;

import java.util.List;

/**
 * Result of {@link RemoteAreaPlanCalculator}: how many sprinklers make up the remote area, how
 * they are arranged, and the pressure the first one starts at. <b>Units:</b> ft2, feet, psi, gpm.
 *
 * <p>Ordered the way the work is done by hand -- area, then rectangle, then counts, then the first
 * sprinkler -- so each figure can be followed from the one before it.
 *
 * @param baseDesignAreaSqFt          the design area as given
 * @param reductionPercent            quick-response ceiling-height reduction taken, {@code 0} if none
 * @param reductionApplied            whether a reduction was actually taken
 * @param adjustedDesignAreaSqFt      the area of sprinkler operation the rest of the plan works to
 * @param remoteAreaLengthFt          the dimension parallel to the branch lines: {@code 1.2 * sqrt(A)}
 * @param remoteAreaWidthFt           the other dimension, {@code A / length}
 * @param coveragePerSprinklerSqFt    {@code sprinklerSpacing * branchSpacing}
 * @param sprinklersPerBranch         {@code ceil(length / sprinklerSpacing)} -- rounded UP
 * @param branchLines                 branch lines the remote area spans
 * @param totalSprinklers             {@code sprinklersPerBranch * branchLines}
 * @param totalProtectedAreaSqFt      what those sprinklers actually protect,
 *                                    {@code totalSprinklers * coveragePerSprinkler}: at least the
 *                                    adjusted design area, because the counts round up
 * @param minimumFlowPerSprinklerGpm  {@code density * coveragePerSprinkler}
 * @param pressureForMinimumFlowPsi   {@code (minimumFlow / K)^2}: what that flow alone would need
 * @param minimumOperatingPressurePsi the floor applied, from reference data
 * @param firstSprinklerPressurePsi   the larger of the two above
 * @param firstSprinklerFlowGpm       {@code K * sqrt(firstSprinklerPressure)}: what it then discharges
 * @param floorGoverned               true when the minimum operating pressure was used because the
 *                                    required flow needed less
 * @param rulesConfidence             confidence of the remote-area rules used ({@code verified} or not)
 * @param notes                       what was done and what was not checked, in plain language
 * @param warnings                    things the designer should look at
 */
public record RemoteAreaPlanResult(
		double baseDesignAreaSqFt,
		double reductionPercent,
		boolean reductionApplied,
		double adjustedDesignAreaSqFt,
		double remoteAreaLengthFt,
		double remoteAreaWidthFt,
		double coveragePerSprinklerSqFt,
		int sprinklersPerBranch,
		int branchLines,
		int totalSprinklers,
		double totalProtectedAreaSqFt,
		double minimumFlowPerSprinklerGpm,
		double pressureForMinimumFlowPsi,
		double minimumOperatingPressurePsi,
		double firstSprinklerPressurePsi,
		double firstSprinklerFlowGpm,
		boolean floorGoverned,
		String rulesConfidence,
		List<String> notes,
		List<String> warnings) {

}
