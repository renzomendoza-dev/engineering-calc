package com.renzoproject.calc_api.mechanical.sprinkler;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * One flowing sprinkler in the remote design area.
 *
 * @param nodeId           node in the pipe network this sprinkler discharges from
 * @param kFactor          sprinkler K, gpm/psi^0.5
 * @param coverageAreaSqFt area this sprinkler protects, ft2
 */
public record RemoteAreaSprinklerDto(
		@NotBlank String nodeId,
		@NotNull @Positive Double kFactor,
		@NotNull @Positive Double coverageAreaSqFt) {

}
