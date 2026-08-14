package com.renzoproject.calc_api.electrical.conduitfill;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * HTTP request body for a conduit fill calculation.
 *
 * <p>{@code conduitType} is a plain string, mapped explicitly in {@link ConduitFillMapper} for
 * the same reason as {@link ConductorFillEntryDto#insulationType()} — clearer 400 errors than
 * a raw Jackson enum-binding failure.
 */
public record ConduitFillRequest(
		@NotEmpty List<@Valid ConductorFillEntryDto> conductors,
		@NotBlank String conduitType) {

}
