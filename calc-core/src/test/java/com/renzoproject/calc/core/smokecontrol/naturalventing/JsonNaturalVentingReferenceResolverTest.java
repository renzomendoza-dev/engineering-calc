package com.renzoproject.calc.core.smokecontrol.naturalventing;

import java.util.HashSet;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonNaturalVentingReferenceResolverTest {

	private final JsonNaturalVentingReferenceResolver resolver = new JsonNaturalVentingReferenceResolver();

	private double cd(String key) {
		return resolver.allDischargeCoefficients().stream().filter(row -> row.key().equals(key)).findFirst().orElseThrow()
				.dischargeCoefficient();
	}

	private double flux(String key) {
		return resolver.allCommodities().stream().filter(row -> row.key().equals(key)).findFirst().orElseThrow()
				.heatReleaseRateKwPerM2();
	}

	@Test
	void dischargeCoefficients_matchTable9242() {
		assertEquals(0.55, cd("LOUVER_90"), 1e-12);
		assertEquals(0.55, cd("FLAP_OR_DOOR_55"), 1e-12);
		assertEquals(0.55, cd("DROP_OUT"), 1e-12);
		assertEquals(0.35, cd("FLAP_OR_DOOR_30"), 1e-12);
		assertEquals(0.25, cd("WEATHER_LOUVER_45"), 1e-12);
	}

	@Test
	void theExplanatoryTextRowIsMarkedAsAnnexGuidanceNotTheTable() {
		var annex = resolver.allDischargeCoefficients().stream().filter(row -> row.key().equals("MOST_OPENINGS_45")).findFirst().orElseThrow();

		assertEquals(0.6, annex.dischargeCoefficient(), 1e-12);
		assertEquals("A.9.2.4.2", annex.clause());
		assertEquals(5, resolver.allDischargeCoefficients().stream().filter(row -> row.clause().equals("Table 9.2.4.2")).count());
	}

	@Test
	void commodityHeatReleaseRates_matchTableA826() {
		assertEquals(23, resolver.allCommodities().size());
		assertEquals(1420.0, flux("WOOD_PALLETS_0_46"), 1e-9);
		assertEquals(4000.0, flux("WOOD_PALLETS_1_52"), 1e-9);
		assertEquals(6800.0, flux("WOOD_PALLETS_3_05"), 1e-9);
		assertEquals(10200.0, flux("WOOD_PALLETS_4_88"), 1e-9);
		assertEquals(400.0, flux("MAIL_BAGS_1_52"), 1e-9);
		assertEquals(14200.0, flux("PS_JARS_COMPARTMENTED_4_5"), 1e-9);
		assertEquals(5400.0, flux("PS_TUBS_NESTED_4_2"), 1e-9);
		assertEquals(740.0, flux("METHYL_ALCOHOL"), 1e-9);
		assertEquals(2500.0, flux("GASOLINE"), 1e-9);
		assertEquals(1700.0, flux("KEROSENE"), 1e-9);
		assertEquals(1700.0, flux("FUEL_OIL_NO_2"), 1e-9);
	}

	@Test
	void everyRowIsVerifiedAndEveryRateIsPositive() {
		assertTrue(resolver.allCommodities().stream().allMatch(row -> row.heatReleaseRateKwPerM2() > 0 && row.confidence().equals("verified")));
		assertTrue(resolver.allDischargeCoefficients().stream()
				.allMatch(row -> row.dischargeCoefficient() > 0 && row.dischargeCoefficient() <= 1 && row.confidence().equals("verified")));
	}

	@Test
	void keysAreUniqueAndTheEditionIsNamed() {
		var commodityKeys = new HashSet<String>();
		resolver.allCommodities().forEach(row -> commodityKeys.add(row.key()));
		assertEquals(resolver.allCommodities().size(), commodityKeys.size());
		assertTrue(resolver.edition().contains("2021"));
		assertTrue(Optional.ofNullable(resolver.note()).orElse("").contains("204-14"));
	}

}
