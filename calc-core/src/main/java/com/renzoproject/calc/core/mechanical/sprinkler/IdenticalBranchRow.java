package com.renzoproject.calc.core.mechanical.sprinkler;

/**
 * How one {@link IdenticalBranch} was balanced: the single branch-level line that stands in for
 * the sprinklers and pipes it was never asked to list. <b>Units:</b> psi, gpm.
 *
 * <p>Reads as the hand-calculation sheet writes it:
 * {@code Q = Q_reference * sqrt(P_junction / P_reference)}.
 *
 * @param joinNodeId             the junction the copy arrived at
 * @param referenceNodeId        the last node of the branch it repeats
 * @param referenceJunctionNodeId where the reference branch's own final pipe lands
 * @param sprinklerCount         sprinklers the copy stands for, so a demand can be counted back
 *                               against the remote area plan
 * @param referenceFlowGpm       {@code Q_reference}: flow of the reference branch as worked from its
 *                               own most remote sprinkler, before any balancing
 * @param referencePressurePsi   {@code P_reference}: pressure where that branch arrives, before
 *                               any balancing
 * @param junctionPressurePsi    {@code P_junction}: the pressure the junction settled at
 * @param scaleFactor            {@code sqrt(P_junction / P_reference)}; {@code 1.0} when the copy
 *                               itself governed the junction
 * @param flowGpm                {@code Q}: the flow the copy carries into the junction
 */
public record IdenticalBranchRow(
		String joinNodeId,
		String referenceNodeId,
		String referenceJunctionNodeId,
		int sprinklerCount,
		double referenceFlowGpm,
		double referencePressurePsi,
		double junctionPressurePsi,
		double scaleFactor,
		double flowGpm) {

}
