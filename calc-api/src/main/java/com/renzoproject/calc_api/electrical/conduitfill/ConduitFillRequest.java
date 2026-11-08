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
 *
 * <p>{@code nipple} (optional, left out meaning false) marks the run as a conduit or tubing nipple of not
 * more than 600 mm between boxes, cabinets or similar enclosures, which PEC Chapter 10 note (4) lets be
 * filled to 60 percent of its area. A nullable {@code Boolean} so a request without it stays valid.
 */
public record ConduitFillRequest(
		@NotEmpty List<@Valid ConductorFillEntryDto> conductors,
		@NotBlank String conduitType,
		Boolean nipple) {

}
