package com.renzoproject.calc_api.mechanical.sprinkler;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

/**
 * HTTP request body for a sprinkler hydraulic calculation. <b>Units:</b> psi, US gpm, feet,
 * inches, ft2 -- NFPA 13's own units, matching calc-core's {@code mechanical.sprinkler} package
 * and the fire pump endpoints, rather than the SI used by the pipe/duct endpoints.
 *
 * <p>Bean Validation here covers structural bounds only. The rules that depend on more than one
 * field -- hose stream node required when the allowance is non-zero, network must be a tree,
 * residual below static, a fitting needing either a type or an explicit length -- are enforced by
 * calc-core and surface as 400s through {@code GlobalExceptionHandler}, so they are not duplicated
 * at this layer.
 *
 * <p>{@code densityGpmPerSqFt}/{@code designAreaSqFt}/{@code hoseStreamGpm} are supplied rather
 * than derived from {@code hazard}: the NFPA 13 density/area curve offers a range of valid design
 * points. {@code GET /api/mechanical/reference/sprinkler-hazard-presets-table} publishes the usual
 * starting points for a form to populate these from.
 *
 * @param systemType              wet, dry or preaction -- affects the C factor a caller should use
 * @param hazard                  hazard the design density came from
 * @param densityGpmPerSqFt       design density
 * @param designAreaSqFt          area of sprinkler operation the remote area represents
 * @param hoseStreamGpm           hose stream allowance; pass {@code 0} for none
 * @param hoseStreamNodeId        node the hose allowance is taken at (not the remote sprinkler)
 * @param assumedStartPressurePsi assumed starting pressure at the most remote sprinkler; the
 *                                minimum-flow pressure wins if it is higher. Pass {@code 0} to let
 *                                the minimum flow govern outright
 * @param sprinklers              flowing sprinklers in the remote area
 * @param segments                pipe network, each segment pointing toward the supply
 * @param waterSupply             where the water comes from: a flow test or an elevated tank.
 *                                <b>Optional</b> -- omit it for a system with no pressurised
 *                                source yet (a cistern awaiting a pump) and the response carries
 *                                the demand figures with a null margin, verdict and supply curve
 * @param deviceLosses            fixed device losses; may be omitted
 */
public record SprinklerHydraulicRequest(
		@NotNull SprinklerSystemTypeDto systemType,
		@NotNull SprinklerHazardDto hazard,
		@NotNull @Positive Double densityGpmPerSqFt,
		@NotNull @Positive Double designAreaSqFt,
		@NotNull @PositiveOrZero Double hoseStreamGpm,
		String hoseStreamNodeId,
		@NotNull @PositiveOrZero Double assumedStartPressurePsi,
		@NotEmpty @Valid List<RemoteAreaSprinklerDto> sprinklers,
		@NotEmpty @Valid List<SprinklerPipeSegmentDto> segments,
		@Valid WaterSupplyDto waterSupply,
		@Valid List<DeviceLossDto> deviceLosses) {

}
