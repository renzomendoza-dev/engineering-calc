package com.renzoproject.calc_api.mechanical.pump;

import com.renzoproject.calc_api.mechanical.pipe.PipePressureLossResponse;

/**
 * {@code result} embeds the existing {@link PipePressureLossResponse} directly (velocity,
 * Reynolds number, flow regime, friction factor, head loss, pressure loss) rather than
 * redeclaring those fields on a new type.
 */
public record SegmentLossDetailDto(PipeSegmentSpecDto segment, PipePressureLossResponse result) {

}
