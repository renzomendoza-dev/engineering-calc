package com.renzoproject.calc.core.mechanical.pump;

/**
 * Where the water source sits relative to the pump: {@link #FLOODED} (above it, positive suction)
 * or {@link #LIFT} (below it, the pump must lift water to its inlet).
 *
 * <p>Shared by {@code PumpTDHCalculator} and the fire pump suite's {@code FirePumpDemandCalculator}.
 * Both use the same physical distinction and only differ in how they quantify the suction side
 * (static head vs. available suction pressure), so there is one enum rather than a copy per
 * package.
 */
public enum SuctionCondition {
	FLOODED,
	LIFT
}
