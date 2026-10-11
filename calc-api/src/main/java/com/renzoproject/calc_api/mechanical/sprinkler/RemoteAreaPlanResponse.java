package com.renzoproject.calc_api.mechanical.sprinkler;

import com.renzoproject.calc.core.mechanical.sprinkler.RemoteAreaPlanResult;

import java.util.List;

/**
 * HTTP response body for a remote area plan, mirroring calc-core's {@link RemoteAreaPlanResult}.
 * <b>Units:</b> ft2, feet, psi, gpm. Ordered the way the work is done by hand.
 *
 * @param baseDesignAreaSqFt          the design area as given
 * @param reductionPercent            quick-response ceiling-height reduction taken; 0 if none
 * @param reductionApplied            whether a reduction was actually taken
 * @param adjustedDesignAreaSqFt      the area the rest of the plan works to
 * @param remoteAreaLengthFt          dimension parallel to the branch lines, 1.2 x sqrt(area)
 * @param remoteAreaWidthFt           the other dimension
 * @param coveragePerSprinklerSqFt    sprinkler spacing times branch spacing
 * @param sprinklersPerBranch         sprinklers on one branch line, rounded up
 * @param branchLines                 branch lines the remote area spans
 * @param totalSprinklers             sprinklersPerBranch times branchLines
 * @param totalProtectedAreaSqFt      what those sprinklers protect; at least the adjusted area
 * @param minimumFlowPerSprinklerGpm  density times one sprinkler's coverage
 * @param pressureForMinimumFlowPsi   what that flow alone needs, (Q/K)^2
 * @param minimumOperatingPressurePsi the floor applied
 * @param firstSprinklerPressurePsi   the larger of the two above
 * @param firstSprinklerFlowGpm       what the first sprinkler then discharges
 * @param floorGoverned               true when the floor was used because the flow needed less
 * @param rulesConfidence             confidence of the layout rules ("verified" or not)
 * @param notes                       what was done and what was not checked
 * @param warnings                    things the designer should look at
 */
public record RemoteAreaPlanResponse(
		Double baseDesignAreaSqFt,
		Double reductionPercent,
		Boolean reductionApplied,
		Double adjustedDesignAreaSqFt,
		Double remoteAreaLengthFt,
		Double remoteAreaWidthFt,
		Double coveragePerSprinklerSqFt,
		Integer sprinklersPerBranch,
		Integer branchLines,
		Integer totalSprinklers,
		Double totalProtectedAreaSqFt,
		Double minimumFlowPerSprinklerGpm,
		Double pressureForMinimumFlowPsi,
		Double minimumOperatingPressurePsi,
		Double firstSprinklerPressurePsi,
		Double firstSprinklerFlowGpm,
		Boolean floorGoverned,
		String rulesConfidence,
		List<String> notes,
		List<String> warnings) {

	public static RemoteAreaPlanResponse from(RemoteAreaPlanResult result) {
		return new RemoteAreaPlanResponse(
				result.baseDesignAreaSqFt(),
				result.reductionPercent(),
				result.reductionApplied(),
				result.adjustedDesignAreaSqFt(),
				result.remoteAreaLengthFt(),
				result.remoteAreaWidthFt(),
				result.coveragePerSprinklerSqFt(),
				result.sprinklersPerBranch(),
				result.branchLines(),
				result.totalSprinklers(),
				result.totalProtectedAreaSqFt(),
				result.minimumFlowPerSprinklerGpm(),
				result.pressureForMinimumFlowPsi(),
				result.minimumOperatingPressurePsi(),
				result.firstSprinklerPressurePsi(),
				result.firstSprinklerFlowGpm(),
				result.floorGoverned(),
				result.rulesConfidence(),
				result.notes(),
				result.warnings());
	}

}
