package com.renzoproject.calc_api.mechanical.sprinkler;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * An elevated tank feeding the system by gravity, mirroring calc-core's
 * {@code GravityTankSupply}.
 *
 * <p>The height gives the pressure at no flow (0.433 psi per foot); the outlet runs are what take
 * it away as flow rises. Omitting the runs is allowed and credits the full head at every flow,
 * which the calculation warns about rather than silently accepting.
 *
 * <p>A tank at or below the connection is rejected by calc-core: it has no pressure to offer
 * there, so it is a pump's source rather than a supply, and such a system is calculated by
 * omitting {@code waterSupply} entirely.
 *
 * @param waterLevelHeightFt height of the water level above the connection point, ft
 * @param outletRuns         pipe runs from the tank down to the connection; may be omitted
 */
public record GravityTankSupplyDto(
		@NotNull @Positive Double waterLevelHeightFt,
		@Valid List<SupplyPipeRunDto> outletRuns) {

}
