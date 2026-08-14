package com.renzoproject.calc.core.electrical.reference;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One row of PEC Table 10.1.1.8 — Conductor Properties, DC Resistance at 75C. Values are
 * Ohms per 305 meters, as published; {@link ConductorDcResistanceTable} converts to ohms/meter
 * on lookup.
 *
 * <p>Some sizes publish two rows — solid (1 strand) and stranded (7+ strands) — distinguished
 * by {@link #stranded()}.
 */
public record ConductorDcResistanceEntry(
		@JsonProperty("sizeLabel") String sizeLabel,
		@JsonProperty("stranded") boolean stranded,
		@JsonProperty("strandCount") int strandCount,
		@JsonProperty("rCuUncoated") double copperUncoatedOhmsPer305m,
		@JsonProperty("rCuCoated") double copperCoatedOhmsPer305m,
		@JsonProperty("rAluminum") double aluminumOhmsPer305m) {

}
