package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The validated network shape: who feeds whom, where the supply is, and which nodes have
 * sprinklers. Built once per calculation so the recursive solve can stay simple.
 */
record SprinklerNetwork(
		String supplyNodeId,
		Map<String, List<SprinklerPipeSegment>> incomingByNode,
		Map<String, SprinklerPipeSegment> outgoingByNode,
		Map<String, RemoteAreaSprinkler> sprinklersByNode,
		Map<String, List<IdenticalBranch>> copiesByJoinNode) {

	static SprinklerNetwork build(SprinklerHydraulicInput input) {
		Map<String, SprinklerPipeSegment> outgoingByNode = new LinkedHashMap<>();
		Map<String, List<SprinklerPipeSegment>> incomingByNode = new LinkedHashMap<>();
		Set<String> nodes = new LinkedHashSet<>();

		for (SprinklerPipeSegment segment : input.segments()) {
			SprinklerPipeSegment existing = outgoingByNode.putIfAbsent(segment.fromNodeId(), segment);
			if (existing != null) {
				throw new CalculationException("Node " + segment.fromNodeId() + " has more than one outgoing "
						+ "segment (to " + existing.toNodeId() + " and to " + segment.toNodeId() + ") -- this "
						+ "calculator solves tree networks only, where flow from a node has exactly one path "
						+ "toward the supply");
			}
			incomingByNode.computeIfAbsent(segment.toNodeId(), key -> new ArrayList<>()).add(segment);
			nodes.add(segment.fromNodeId());
			nodes.add(segment.toNodeId());
		}

		List<String> terminals = nodes.stream().filter(node -> !outgoingByNode.containsKey(node)).toList();
		if (terminals.size() != 1) {
			throw new CalculationException("The network must have exactly one supply point (a node with no "
					+ "outgoing segment), but found " + terminals.size() + ": " + terminals
					+ " -- check the segment directions, which must all point toward the supply");
		}
		String supplyNodeId = terminals.get(0);

		for (String node : nodes) {
			Set<String> visited = new LinkedHashSet<>();
			String current = node;
			while (current != null && !current.equals(supplyNodeId)) {
				if (!visited.add(current)) {
					throw new CalculationException("The network contains a loop through node " + current
							+ " -- this calculator solves tree networks only; a looped or gridded system needs "
							+ "a simultaneous network solution it does not implement");
				}
				SprinklerPipeSegment next = outgoingByNode.get(current);
				current = next == null ? null : next.toNodeId();
			}
		}

		Map<String, RemoteAreaSprinkler> sprinklersByNode = new LinkedHashMap<>();
		for (RemoteAreaSprinkler sprinkler : input.sprinklers()) {
			if (!nodes.contains(sprinkler.nodeId())) {
				throw new CalculationException("Sprinkler node " + sprinkler.nodeId() + " is not an end of any "
						+ "pipe segment -- every sprinkler must be connected to the network");
			}
			sprinklersByNode.put(sprinkler.nodeId(), sprinkler);
		}

		if (input.hoseStreamGpm() > 0 && !nodes.contains(input.hoseStreamNodeId())) {
			throw new CalculationException("Hose stream node " + input.hoseStreamNodeId() + " is not an end of "
					+ "any pipe segment");
		}

		Map<String, List<IdenticalBranch>> copiesByJoinNode = new LinkedHashMap<>();
		for (IdenticalBranch copy : input.identicalBranches()) {
			if (!nodes.contains(copy.joinNodeId())) {
				throw new CalculationException("An identical branch joins at node " + copy.joinNodeId()
						+ ", which is not an end of any pipe segment -- it has to arrive at a junction of the "
						+ "network");
			}
			if (!outgoingByNode.containsKey(copy.referenceNodeId())) {
				throw new CalculationException("The identical branch joining at " + copy.joinNodeId()
						+ " repeats node " + copy.referenceNodeId() + ", which has no pipe leaving it -- name "
						+ "the last node of the branch to repeat, the one its final pipe leaves from");
			}
			if (isUpstreamOf(copy.joinNodeId(), copy.referenceNodeId(), incomingByNode)) {
				throw new CalculationException("The identical branch joining at " + copy.joinNodeId()
						+ " repeats " + copy.referenceNodeId() + ", which is fed through " + copy.joinNodeId()
						+ " itself -- a branch cannot be a copy of something it is part of");
			}
			copiesByJoinNode.computeIfAbsent(copy.joinNodeId(), key -> new ArrayList<>()).add(copy);
		}

		return new SprinklerNetwork(supplyNodeId, incomingByNode, outgoingByNode, sprinklersByNode, copiesByJoinNode);
	}

	/** Whether {@code candidate} is {@code node} or anything that feeds it, by the listed pipes. */
	private static boolean isUpstreamOf(
			String candidate, String node, Map<String, List<SprinklerPipeSegment>> incomingByNode) {
		if (candidate.equals(node)) {
			return true;
		}
		for (SprinklerPipeSegment segment : incomingByNode.getOrDefault(node, List.of())) {
			if (isUpstreamOf(candidate, segment.fromNodeId(), incomingByNode)) {
				return true;
			}
		}
		return false;
	}

	List<SprinklerPipeSegment> incoming(String nodeId) {
		return incomingByNode.getOrDefault(nodeId, List.of());
	}

	Optional<RemoteAreaSprinkler> sprinklerAt(String nodeId) {
		return Optional.ofNullable(sprinklersByNode.get(nodeId));
	}

	/** The one pipe leaving a node; only asked of nodes already checked to have one. */
	SprinklerPipeSegment outgoing(String nodeId) {
		return outgoingByNode.get(nodeId);
	}

	List<IdenticalBranch> copiesJoiningAt(String nodeId) {
		return copiesByJoinNode.getOrDefault(nodeId, List.of());
	}

}
