package com.renzoproject.calc.core.electrical.wiresizing;

import com.renzoproject.calc.core.electrical.reference.ConduitMaterial;
import com.renzoproject.calc.core.electrical.voltagedrop.CircuitType;

/**
 * Optional voltage drop cross-check for {@link WireSizingInput}.
 *
 * <p>Mirrors the fields {@code VoltageDropInput} needs beyond current and resistance/reactance
 * — current comes from {@code WireSizingInput.loadCurrentAmps()}, and resistance/reactance are
 * resolved per candidate conductor size during the upsizing walk in
 * {@link WireSizingCalculator}, not supplied here.
 */
public record VoltageDropCheckRequest(
		CircuitType circuitType,
		double oneWayLengthMeters,
		double powerFactor,
		double systemVoltage,
		ConduitMaterial conduitMaterial,
		int parallelSetsPerPhase) {

}
