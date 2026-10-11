package com.renzoproject.calc_api.mechanical.sprinkler;

import com.renzoproject.calc.core.mechanical.sprinkler.RemoteAreaPlanResult;

import java.util.List;

/**
 * HTTP response body for a remote area plan, mirroring calc-core's {@link RemoteAreaPlanResult}.
 * <b>Units:</b> ft2, feet, psi, gpm. Ordered the way the work is done by hand.
 *
 * <p>In three tiers that become available as the request's inputs do: the area and per-sprinkler
 * figures always; the layout fields ({@code branchSpacingFt} through
 * {@code totalProtectedAreaSqFt}) only once {@code sprinklerSpacingFt} was given, and
 * <b>null</b> before then.
 *
 * @param baseDesignAreaSqFt          the design area as given
 * @param reductionPercent            quick-response ceiling-height reduction taken; 0 if none
 * @param reductionApplied            whether a reduction was actually taken
 * @param adjustedDesignAreaSqFt      the area the rest of the plan works to
 * @param coveragePerSprinklerSqFt    the area each sprinkler protects, as given
 * @param sprinklersForArea           fewest sprinklers that cover the adjusted area, rounded up
 * @param flowPerSprinklerGpm         density times one sprinkler's coverage
 * @param pressureForFlowPsi          what that flow alone needs, (Q/K)^2
 * @param minimumOperatingPressurePsi the floor applied
 * @param firstSprinklerPressurePsi   the larger of the two above
 * @param firstSprinklerFlowGpm       what the first sprinkler then discharges
 * @param floorGoverned               true when the floor was used because the flow needed less
 * @param remoteAreaLengthFt          dimension parallel to the branch lines, 1.2 x sqrt(area)
 * @param remoteAreaWidthFt           the other dimension
 * @param branchSpacingFt             spacing between branch lines; null without the spacing
 * @param sprinklersPerBranch         sprinklers on one branch line, rounded up; null without the spacing
 * @param branchLines                 branch lines the remote area spans; null without the spacing
 * @param totalSprinklers             sprinklersPerBranch times branchLines; null without the spacing
 * @param totalProtectedAreaSqFt      what those sprinklers protect; null without the spacing
 * @param rulesConfidence             confidence of the layout rules ("verified" or not)
 * @param notes                       what was done and what was not checked
 * @param warnings                    things the designer should look at
 */
public record RemoteAreaPlanResponse(
		Double baseDesignAreaSqFt,
		Double reductionPercent,
		Boolean reductionApplied,
		Double adjustedDesignAreaSqFt,
		Double coveragePerSprinklerSqFt,
		Integer sprinklersForArea,
		Double flowPerSprinklerGpm,
		Double pressureForFlowPsi,
		Double minimumOperatingPressurePsi,
		Double firstSprinklerPressurePsi,
		Double firstSprinklerFlowGpm,
		Boolean floorGoverned,
		Double remoteAreaLengthFt,
		Double remoteAreaWidthFt,
		Double branchSpacingFt,
		Integer sprinklersPerBranch,
		Integer branchLines,
		Integer totalSprinklers,
		Double totalProtectedAreaSqFt,
		String rulesConfidence,
		List<String> notes,
		List<String> warnings) {

	public static RemoteAreaPlanResponse from(RemoteAreaPlanResult result) {
		return new RemoteAreaPlanResponse(
				result.baseDesignAreaSqFt(),
				result.reductionPercent(),
				result.reductionApplied(),
				result.adjustedDesignAreaSqFt(),
				result.coveragePerSprinklerSqFt(),
				result.sprinklersForArea(),
				result.flowPerSprinklerGpm(),
				result.pressureForFlowPsi(),
				result.minimumOperatingPressurePsi(),
				result.firstSprinklerPressurePsi(),
				result.firstSprinklerFlowGpm(),
				result.floorGoverned(),
				result.remoteAreaLengthFt(),
				result.remoteAreaWidthFt(),
				result.branchSpacingFt(),
				result.sprinklersPerBranch(),
				result.branchLines(),
				result.totalSprinklers(),
				result.totalProtectedAreaSqFt(),
				result.rulesConfidence(),
				result.notes(),
				result.warnings());
	}

}
