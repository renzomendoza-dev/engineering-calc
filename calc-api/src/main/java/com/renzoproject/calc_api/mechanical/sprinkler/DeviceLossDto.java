package com.renzoproject.calc_api.mechanical.sprinkler;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * A fixed device pressure loss (backflow preventer, alarm valve, strainer, meter), added before
 * the requirement is compared against the supply.
 *
 * @param name            device label, echoed in the readout
 * @param pressureLossPsi loss at the system's demand flow
 */
public record DeviceLossDto(
		@NotBlank String name,
		@NotNull @PositiveOrZero Double pressureLossPsi) {

}
