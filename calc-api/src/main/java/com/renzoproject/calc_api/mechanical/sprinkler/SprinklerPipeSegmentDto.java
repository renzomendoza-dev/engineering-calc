package com.renzoproject.calc_api.mechanical.sprinkler;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

/**
 * One pipe run. <b>Direction matters:</b> {@code fromNodeId} is the upstream end and
 * {@code toNodeId} points toward the supply; the network's shape is derived from these pairs.
 *
 * @param fromNodeId        upstream node
 * @param toNodeId          downstream node, toward the supply
 * @param pipeMaterial      material code from the pipe reference data (e.g. {@code BI})
 * @param schedule          schedule code (e.g. {@code SCH40})
 * @param nominalSize       nominal size key (e.g. {@code "2"}, {@code "1-1/4"}); the actual inside
 *                          diameter is resolved from reference data
 * @param cFactor           Hazen-Williams C for this segment
 * @param pipeLengthFt      centerline pipe length, ft
 * @param elevationChangeFt rise from {@code fromNodeId} to {@code toNodeId}, ft; negative for a drop
 * @param fittings          fittings on this segment; may be omitted
 */
public record SprinklerPipeSegmentDto(
		@NotBlank String fromNodeId,
		@NotBlank String toNodeId,
		@NotBlank String pipeMaterial,
		String schedule,
		@NotBlank String nominalSize,
		@NotNull @Positive Double cFactor,
		@NotNull @PositiveOrZero Double pipeLengthFt,
		@NotNull Double elevationChangeFt,
		@Valid List<SegmentFittingDto> fittings) {

}
