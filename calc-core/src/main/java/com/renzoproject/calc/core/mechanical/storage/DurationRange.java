package com.renzoproject.calc.core.mechanical.storage;

/**
 * NFPA 13 water supply duration requirement for one hazard classification.
 *
 * @param minMinutes             lower bound; equals {@code maxMinutes} for Light Hazard, which
 *                               NFPA 13 doesn't subdivide further
 * @param maxMinutes             upper bound
 * @param hoseStreamAllowanceGpm combined hose stream allowance, GPM -- exposed for future use;
 *                               {@link FireWaterStorageCalculator} deliberately does NOT add this
 *                               to its volume calculation (see that class's Javadoc)
 */
public record DurationRange(double minMinutes, double maxMinutes, double hoseStreamAllowanceGpm) {

}
