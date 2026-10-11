package com.renzoproject.calc_api.mechanical.sprinkler;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

/**
 * One pipe run between a gravity tank and the connection point, mirroring calc-core's
 * {@code SupplyPipeRun}.
 *
 * <p>Unlike {@link SprinklerPipeSegmentDto} there are no node ids (the supply side is one path,
 * not a tree) and no elevation change: the tank's height is stated once on
 * {@link GravityTankSupplyDto}, and repeating it here would count the same head twice.
 *
 * @param pipeMaterial material code, e.g. {@code BI}
 * @param schedule     schedule code, e.g. {@code SCH40}; optional
 * @param nominalSize  nominal size key as published in the pipe reference data
 * @param cFactor      Hazen-Williams C for this run
 * @param pipeLengthFt centerline length, ft
 * @param fittings     fittings on this run; may be omitted
 */
public record SupplyPipeRunDto(
		@NotBlank String pipeMaterial,
		String schedule,
		@NotBlank String nominalSize,
		@NotNull @Positive Double cFactor,
		@NotNull @PositiveOrZero Double pipeLengthFt,
		@Valid List<SegmentFittingDto> fittings) {

}
