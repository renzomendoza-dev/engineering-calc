package com.renzoproject.calc.core.electrical.motorflc;

/**
 * Input parameters for {@link LockedRotorCalculator}.
 *
 * @param isPolyphase     {@code true} for polyphase (Design B/C/D) motors, {@code false} for
 *                        single-phase
 * @param horsepowerLabel horsepower label matching a PEC table's size notation exactly
 * @param voltage         motor voltage
 */
public record LockedRotorInput(boolean isPolyphase, String horsepowerLabel, int voltage) {

}
