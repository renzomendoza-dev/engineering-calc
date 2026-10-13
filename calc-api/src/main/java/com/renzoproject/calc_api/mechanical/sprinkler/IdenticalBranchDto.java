package com.renzoproject.calc_api.mechanical.sprinkler;

import jakarta.validation.constraints.NotBlank;

/**
 * A branch line declared a copy of another instead of listed sprinkler by sprinkler. Mirrors
 * calc-core's {@code IdenticalBranch}.
 *
 * <p>Nothing about the copy is listed except where it arrives: the cross main that carries it on is
 * ordinary segments, so each copy keeps its own distance to its neighbour.
 *
 * @param joinNodeId      the junction the copy arrives at; must be a node of the network
 * @param referenceNodeId the last node of the branch being repeated -- the one its final pipe
 *                        leaves from. That branch is the node, everything feeding it, and the one
 *                        segment leaving it. Must not be fed through {@code joinNodeId}
 */
public record IdenticalBranchDto(@NotBlank String joinNodeId, @NotBlank String referenceNodeId) {

}
