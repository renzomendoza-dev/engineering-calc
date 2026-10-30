package com.renzoproject.calc.core.smokecontrol.naturalventing;

import com.renzoproject.calc.core.exception.CalculationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Input for {@link NaturalVentingCalculator}: NFPA 204 (2021) natural smoke and heat venting,
 * hand-calculation method of Section 9.2, for any number of curtained areas and design fires.
 *
 * @param ambientTemperature      To, degC
 * @param ambientDensity          rho_o, kg/m3; nullable, defaults to the ideal-gas value at {@code To}
 *                                (Annex D uses 1.2 at 294 K)
 * @param specificHeat            cp of the smoke layer gases, kJ/kg-K; nullable, defaults to 1.00 (Annex D)
 * @param layerEnergyFraction     K, fraction of the convected energy in the smoke layer (9.2.4.4);
 *                                nullable, defaults to 0.5, the only value allowed without an
 *                                analysis acceptable to the AHJ
 * @param growthConstant          numerator of {@code Q = c (t/tg)^2} (8.3.1); nullable, defaults to
 *                                1055. Annex A.8.3.2 and Annex D print 1000 instead
 * @param zones                   at least one
 * @param fireCases               at least one; each names one of the zones
 */
public record NaturalVentingInput(
		double ambientTemperature,
		Double ambientDensity,
		Double specificHeat,
		Double layerEnergyFraction,
		Double growthConstant,
		List<VentZone> zones,
		List<FireCase> fireCases) {

	public static final double DEFAULT_SPECIFIC_HEAT = 1.00;
	public static final double DEFAULT_LAYER_ENERGY_FRACTION = 0.5;
	public static final double DEFAULT_GROWTH_CONSTANT = 1055.0;

	public NaturalVentingInput {
		if (ambientTemperature <= -273.15) {
			throw new CalculationException("ambientTemperature must be above absolute zero");
		}
		if (ambientDensity == null) {
			ambientDensity = 101.325 / (0.287 * (ambientTemperature + 273.15));
		}
		if (ambientDensity <= 0) {
			throw new CalculationException("ambientDensity must be positive");
		}
		if (specificHeat == null) {
			specificHeat = DEFAULT_SPECIFIC_HEAT;
		}
		if (specificHeat <= 0) {
			throw new CalculationException("specificHeat must be positive");
		}
		if (layerEnergyFraction == null) {
			layerEnergyFraction = DEFAULT_LAYER_ENERGY_FRACTION;
		}
		if (layerEnergyFraction <= 0 || layerEnergyFraction > 1) {
			throw new CalculationException("layerEnergyFraction must be greater than 0 and at most 1");
		}
		if (growthConstant == null) {
			growthConstant = DEFAULT_GROWTH_CONSTANT;
		}
		if (growthConstant <= 0) {
			throw new CalculationException("growthConstant must be positive");
		}
		if (zones == null || zones.isEmpty()) {
			throw new CalculationException("at least one zone is required");
		}
		if (fireCases == null || fireCases.isEmpty()) {
			throw new CalculationException("at least one fire case is required");
		}
		zones = List.copyOf(zones);
		fireCases = List.copyOf(fireCases);
		Set<String> zoneNames = new HashSet<>();
		for (VentZone zone : zones) {
			if (!zoneNames.add(zone.name())) {
				throw new CalculationException("zone name used twice: " + zone.name());
			}
		}
		Set<String> caseNames = new HashSet<>();
		for (FireCase fireCase : fireCases) {
			if (!caseNames.add(fireCase.name())) {
				throw new CalculationException("fire case name used twice: " + fireCase.name());
			}
			if (!zoneNames.contains(fireCase.zoneName())) {
				throw new CalculationException("fire case " + fireCase.name() + " refers to an unknown zone: " + fireCase.zoneName());
			}
		}
	}

}
