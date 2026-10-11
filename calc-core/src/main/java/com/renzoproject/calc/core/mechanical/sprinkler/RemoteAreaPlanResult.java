package com.renzoproject.calc.core.mechanical.sprinkler;

import java.util.List;

/**
 * Result of {@link RemoteAreaPlanCalculator}: the area to protect, what each sprinkler flows and
 * starts at, and -- once the spacing is known -- how the sprinklers are laid out.
 * <b>Units:</b> ft2, feet, psi, gpm.
 *
 * <p>Ordered the way the work is done by hand, and in three tiers that become available as the
 * inputs do: the area and the per-sprinkler figures need only the design basis; the shape of the
 * remote area needs only that area; the layout needs the spacing along the branch too, and its
 * fields are {@code null} until then.
 *
 * @param baseDesignAreaSqFt          the design area as given
 * @param reductionPercent            quick-response ceiling-height reduction taken, {@code 0} if none
 * @param reductionApplied            whether a reduction was actually taken
 * @param adjustedDesignAreaSqFt      the area of sprinkler operation the rest of the plan works to
 * @param coveragePerSprinklerSqFt    the area each sprinkler protects, as given
 * @param sprinklersForArea           {@code ceil(adjusted area / coverage)}: the fewest sprinklers
 *                                    that cover the area. The layout can only equal or exceed it,
 *                                    because whole sprinklers per branch round up again
 * @param flowPerSprinklerGpm         {@code density * coverage}: what each sprinkler must discharge
 * @param pressureForFlowPsi          {@code (flow / K)^2}: what that flow alone would need
 * @param minimumOperatingPressurePsi the floor applied, from reference data
 * @param firstSprinklerPressurePsi   the larger of the two above
 * @param firstSprinklerFlowGpm       {@code K * sqrt(firstSprinklerPressure)}: what it then discharges
 * @param floorGoverned               true when the minimum operating pressure was used because the
 *                                    required flow needed less
 * @param remoteAreaLengthFt          the dimension parallel to the branch lines: {@code 1.2 * sqrt(A)}
 * @param remoteAreaWidthFt           the other dimension, {@code A / length}
 * @param branchSpacingFt             spacing between branch lines, {@code coverage / spacingAlongBranch};
 *                                    <b>null</b> without the spacing
 * @param sprinklersPerBranch         {@code ceil(length / spacing)} -- rounded UP; <b>null</b> without
 *                                    the spacing
 * @param branchLines                 branch lines the remote area spans; <b>null</b> without the spacing
 * @param totalSprinklers             {@code sprinklersPerBranch * branchLines}; <b>null</b> without
 *                                    the spacing
 * @param totalProtectedAreaSqFt      what those sprinklers actually protect: at least the adjusted
 *                                    design area, because the counts round up; <b>null</b> without
 *                                    the spacing
 * @param rulesConfidence             confidence of the remote-area rules used ({@code verified} or not)
 * @param notes                       what was done and what was not checked, in plain language
 * @param warnings                    things the designer should look at
 */
public record RemoteAreaPlanResult(
		double baseDesignAreaSqFt,
		double reductionPercent,
		boolean reductionApplied,
		double adjustedDesignAreaSqFt,
		double coveragePerSprinklerSqFt,
		int sprinklersForArea,
		double flowPerSprinklerGpm,
		double pressureForFlowPsi,
		double minimumOperatingPressurePsi,
		double firstSprinklerPressurePsi,
		double firstSprinklerFlowGpm,
		boolean floorGoverned,
		double remoteAreaLengthFt,
		double remoteAreaWidthFt,
		Double branchSpacingFt,
		Integer sprinklersPerBranch,
		Integer branchLines,
		Integer totalSprinklers,
		Double totalProtectedAreaSqFt,
		String rulesConfidence,
		List<String> notes,
		List<String> warnings) {

	/** True once the spacing was given and the layout worked out. */
	public boolean hasLayout() {
		return totalSprinklers != null;
	}

}
