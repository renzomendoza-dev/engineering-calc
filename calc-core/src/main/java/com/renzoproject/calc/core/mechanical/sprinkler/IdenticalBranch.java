package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * A branch line that is hydraulically a copy of another, declared instead of listed.
 *
 * <p>The usual hand-calculation shortcut: when several branches have the same sprinklers on the
 * same pipes, the second and later ones need not be worked sprinkler by sprinkler. Each is the
 * reference branch's own result, balanced at its own junction -- its flow is the reference flow
 * scaled by {@code sqrt(P_junction / P_reference)}, exactly as a branch entered one by one would
 * be. Its distance to its neighbour is not part of the copy: the cross main that carries it on is
 * ordinary pipe, listed as segments like any other.
 *
 * <p><b>What "the branch" is.</b> {@code referenceNodeId} names the node the reference branch's
 * <i>final pipe leaves from</i> -- its last sprinkler, in practice. The branch is that node, every
 * pipe and sprinkler feeding it, and the one segment leaving it, which lands on the reference's
 * junction. The copy lands on {@code joinNodeId} in place of that junction.
 *
 * <p>The copy is of the reference as worked from its own most remote sprinkler, <i>before</i> any
 * balancing at the reference's junction. Balancing scales a branch by a single factor, which keeps
 * {@code Q / sqrt(P)} -- the branch's equivalent K -- unchanged, so copying the balanced figures
 * would give the same answer; the unbalanced ones are simply the cleaner thing to state.
 *
 * @param joinNodeId      the junction the copy arrives at; must be a node of the network
 * @param referenceNodeId the last node of the branch being repeated; must have one outgoing
 *                        segment, and must not be fed through {@code joinNodeId}
 */
public record IdenticalBranch(String joinNodeId, String referenceNodeId) {

	public IdenticalBranch {
		if (joinNodeId == null || joinNodeId.isBlank()) {
			throw new CalculationException("an identical branch needs the node it joins at");
		}
		if (referenceNodeId == null || referenceNodeId.isBlank()) {
			throw new CalculationException("an identical branch joining at " + joinNodeId
					+ " needs the node of the branch it repeats");
		}
		if (joinNodeId.equals(referenceNodeId)) {
			throw new CalculationException("an identical branch cannot repeat the node it joins at ("
					+ joinNodeId + ")");
		}
	}

}
