package com.renzoproject.calc_api.smokecontrol;

/**
 * DTO mirror of calc-core's sealed {@code PlumeRegime} ({@code FarField}/{@code NearField}).
 *
 * <p>Unlike {@code PipeSizingResult}'s DTO mapping (which flattens both modes' fields into one
 * parent response with nulls for whichever mode didn't apply, since its two modes carry
 * genuinely different data), {@code FarField} and {@code NearField} share one identical field
 * ({@code massFlowRateKgS}) — there's nothing to null out per variant, so a small nested object
 * with a {@code type} discriminator is both simpler and a more direct fit here than flattening.
 */
public record PlumeRegimeDto(PlumeRegimeTypeDto type, Double massFlowRate) {

}
