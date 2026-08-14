package com.renzoproject.calc.core.mechanical.firepump;

/**
 * Not part of the prompt's original {@code FirePumpPowerInput} record shapes, but added to
 * {@link RatedPointOnly}/{@link FullCurve} out of necessity: {@link FirePumpPowerResult}'s
 * {@code recommendedElectricMotorSize} ("null if not applicable") and {@code driverSizingNote}
 * ("diesel-driver caveat") both require the calculator to know which kind of driver it's
 * sizing for, and nothing in the originally-specified input types carried that information.
 */
public enum DriverType {
	ELECTRIC,
	DIESEL
}
