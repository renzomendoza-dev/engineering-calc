package com.renzoproject.calc_api.mechanical.standpipe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * One run of pipe on the path from the most remote hose connection to the supply, mirroring
 * calc-core StandpipeSegment. {@code role} is HOSE_PIPING, STANDPIPE, BRANCH_LINE or OTHER;
 * {@code hoseConnectionSize} is ONE_AND_HALF_INCH or TWO_AND_HALF_INCH, or null.
 * {@code otherStandpipeConnection} (may be left out, meaning false) marks the flow added here as
 * another standpipe's, at its connection point to the common supply piping. Units: gpm, ft.
 */
public record StandpipeSegmentDto(
		@NotBlank String label,
		@NotBlank String role,
		Double flowAddedGpm,
		String hoseConnectionSize,
		Boolean otherStandpipeConnection,
		@NotBlank String pipeMaterial,
		String schedule,
		@NotBlank String nominalSize,
		@NotNull Double cFactor,
		@NotNull Double pipeLengthFt,
		@NotNull Double elevationRiseFt,
		List<@Valid StandpipeFittingDto> fittings) {

}
