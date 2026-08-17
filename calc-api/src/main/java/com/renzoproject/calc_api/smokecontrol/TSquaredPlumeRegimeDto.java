package com.renzoproject.calc_api.smokecontrol;

/**
 * DTO mirror of calc-core's sealed {@code TSquaredPlumeRegime} ({@code TSquaredFarField}/
 * {@code TSquaredNearField}). Deliberate duplicate of {@link PlumeRegimeDto} -- see that type's
 * Javadoc for why a small nested {@code type} + shared field object (rather than
 * {@code PipeSizingResult}'s flatten-with-nulls technique) is the right shape here; kept as its
 * own type rather than reused, matching the "fully separate, duplicated" decision already made
 * at the core layer.
 */
public record TSquaredPlumeRegimeDto(TSquaredPlumeRegimeTypeDto type, Double massFlowRate) {

}
