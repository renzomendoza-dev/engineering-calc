package com.renzoproject.calc.core.electrical.motorflc;

import com.renzoproject.calc.core.electrical.wiresizing.WireSizingResult;

/**
 * @param motorFlcResult   "why this current" — the motor's full-load current calculation
 * @param wireSizingResult "why this wire size" — the conductor sizing calculation, using
 *                         {@code motorFlcResult.flcAmps()} as its load current
 */
public record MotorConductorSizingResult(MotorFlcResult motorFlcResult, WireSizingResult wireSizingResult) {

}
