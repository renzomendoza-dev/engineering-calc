package com.renzoproject.calc_api.mechanical.standpipe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * The piping of one fire department connection, mirroring calc-core {@code StandpipeFdcLine}.
 * {@code segments} run from the system to the connection and are plain pipe, so their
 * {@code flowAddedGpm} and {@code hoseConnectionSize} are left out. {@code flowGpm} is the flow the
 * connection carries (left out: the whole flow at the supply) and {@code inlets} the inlets on it.
 */
public record StandpipeFdcLineDto(
		@NotBlank String label,
		Double flowGpm,
		Integer inlets,
		@NotEmpty List<@Valid StandpipeSegmentDto> segments) {

}
