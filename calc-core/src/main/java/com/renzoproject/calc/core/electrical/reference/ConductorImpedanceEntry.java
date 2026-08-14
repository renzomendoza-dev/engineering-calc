package com.renzoproject.calc.core.electrical.reference;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One row of PEC Table 10.1.1.9 — AC Resistance and Reactance for 600V Cables, 3-Phase, 60Hz,
 * 75C, Three Single Conductors in Conduit. All values are Ohms to Neutral per 305 meters, as
 * published; {@link ConductorImpedanceTable} converts to ohms/meter on lookup.
 *
 * <p>Resistance fields are nullable ({@link Double}) because not every material/conduit
 * combination has published data for every size (e.g. aluminum wire has no entry at size
 * {@code "2.0"}).
 */
public record ConductorImpedanceEntry(
		@JsonProperty("sizeLabel") String sizeLabel,
		@JsonProperty("xlPvcAluminum") double reactancePvcAluminumConduitOhmsPer305m,
		@JsonProperty("xlSteel") double reactanceSteelConduitOhmsPer305m,
		@JsonProperty("rCuPvc") Double resistanceCopperPvcOhmsPer305m,
		@JsonProperty("rCuAluminumConduit") Double resistanceCopperAluminumConduitOhmsPer305m,
		@JsonProperty("rCuSteel") Double resistanceCopperSteelOhmsPer305m,
		@JsonProperty("rAlPvc") Double resistanceAluminumWirePvcOhmsPer305m,
		@JsonProperty("rAlAluminumConduit") Double resistanceAluminumWireAluminumConduitOhmsPer305m,
		@JsonProperty("rAlSteel") Double resistanceAluminumWireSteelOhmsPer305m) {

}
