package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;

import java.util.List;

/**
 * One pipe run between two nodes. <b>Direction matters:</b> {@code fromNodeId} is the upstream end
 * (further from the supply, where flow enters this pipe) and {@code toNodeId} is the downstream end
 * (toward the riser/supply). The calculator derives the network's shape from these pairs.
 *
 * @param fromNodeId          upstream node
 * @param toNodeId            downstream node, toward the supply
 * @param pipeMaterial        material code for {@code PipeDimensionResolver} (e.g. {@code BI})
 * @param schedule            schedule code for {@code PipeDimensionResolver} (e.g. {@code SCH40})
 * @param nominalSize         nominal size key as published in {@code reference/pipes/*.json}
 *                            (e.g. {@code "2"}, {@code "1-1/4"}) -- the actual inside diameter is
 *                            resolved from reference data, never assumed from the nominal size
 * @param cFactor             Hazen-Williams C for this segment; must be positive. Supplied per
 *                            segment because a system can mix materials (e.g. steel branch lines
 *                            on a ductile iron underground main)
 * @param pipeLengthFt        centerline pipe length, ft; must not be negative
 * @param elevationChangeFt   rise from {@code fromNodeId} to {@code toNodeId}, ft; negative for a
 *                            drop
 * @param fittings            fittings on this segment; may be empty or null
 * @throws CalculationException if any rule above is violated
 */
public record SprinklerPipeSegment(
		String fromNodeId,
		String toNodeId,
		String pipeMaterial,
		String schedule,
		String nominalSize,
		double cFactor,
		double pipeLengthFt,
		double elevationChangeFt,
		List<SegmentFitting> fittings) {

	public SprinklerPipeSegment {
		if (fromNodeId == null || fromNodeId.isBlank()) {
			throw new CalculationException("segment fromNodeId is required");
		}
		if (toNodeId == null || toNodeId.isBlank()) {
			throw new CalculationException("segment toNodeId is required");
		}
		if (fromNodeId.equals(toNodeId)) {
			throw new CalculationException("segment fromNodeId and toNodeId must differ (both were " + fromNodeId + ")");
		}
		if (pipeMaterial == null || pipeMaterial.isBlank()) {
			throw new CalculationException("pipeMaterial is required for segment " + fromNodeId + "->" + toNodeId);
		}
		if (nominalSize == null || nominalSize.isBlank()) {
			throw new CalculationException("nominalSize is required for segment " + fromNodeId + "->" + toNodeId);
		}
		if (cFactor <= 0) {
			throw new CalculationException("cFactor must be positive for segment " + fromNodeId + "->" + toNodeId);
		}
		if (pipeLengthFt < 0) {
			throw new CalculationException("pipeLengthFt must not be negative for segment " + fromNodeId + "->" + toNodeId);
		}
		fittings = fittings == null ? List.of() : List.copyOf(fittings);
	}

}
