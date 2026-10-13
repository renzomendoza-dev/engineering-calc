package com.renzoproject.calc_api.mechanical.sprinkler;

import com.renzoproject.calc.core.mechanical.sprinkler.IdenticalBranchRow;

/**
 * The one branch-level line that stands in for an identical branch's sprinklers and pipes,
 * mirroring calc-core's {@link IdenticalBranchRow}. <b>Units:</b> psi, gpm.
 *
 * <p>Reads as the hand-calculation sheet writes it:
 * {@code flowGpm = referenceFlowGpm * sqrt(junctionPressurePsi / referencePressurePsi)}.
 *
 * @param joinNodeId              the junction the copy arrived at
 * @param referenceNodeId         the last node of the branch it repeats
 * @param referenceJunctionNodeId where the reference branch's own final pipe lands
 * @param sprinklerCount          sprinklers the copy stands for
 * @param referenceFlowGpm        flow of the reference branch as worked from its own most remote
 *                                sprinkler, before any balancing
 * @param referencePressurePsi    pressure where that branch arrives, before any balancing
 * @param junctionPressurePsi     the pressure the junction settled at
 * @param scaleFactor             {@code sqrt(junctionPressurePsi / referencePressurePsi)}
 * @param flowGpm                 the flow the copy carries into the junction
 */
public record IdenticalBranchRowDto(
		String joinNodeId,
		String referenceNodeId,
		String referenceJunctionNodeId,
		Integer sprinklerCount,
		Double referenceFlowGpm,
		Double referencePressurePsi,
		Double junctionPressurePsi,
		Double scaleFactor,
		Double flowGpm) {

	public static IdenticalBranchRowDto from(IdenticalBranchRow row) {
		return new IdenticalBranchRowDto(
				row.joinNodeId(),
				row.referenceNodeId(),
				row.referenceJunctionNodeId(),
				row.sprinklerCount(),
				row.referenceFlowGpm(),
				row.referencePressurePsi(),
				row.junctionPressurePsi(),
				row.scaleFactor(),
				row.flowGpm());
	}

}
