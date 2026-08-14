package com.renzoproject.calc.core.electrical.motorflc;

/**
 * @param flcAmps                 full-load current after synchronous power factor adjustment,
 *                                 if applicable — same as {@code baseFlcAmps} otherwise
 * @param baseFlcAmps              full-load current as published, before any PF adjustment;
 *                                  kept alongside {@code flcAmps} so the adjustment (if any)
 *                                  is visible rather than hidden
 * @param minimumConductorAmpacity {@code flcAmps * 1.25}, per PEC's continuous-duty
 *                                  single-motor conductor sizing convention
 */
public record MotorFlcResult(double flcAmps, double baseFlcAmps, double minimumConductorAmpacity) {

}
