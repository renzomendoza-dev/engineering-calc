package com.renzoproject.calc.core.smokecontrol.naturalventing;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.smokecontrol.naturalventing.NaturalVentingResult.FireCaseResult;
import com.renzoproject.calc.core.smokecontrol.naturalventing.NaturalVentingResult.Regime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NaturalVentingCalculatorTest {

	/** 21 degC in Annex D is 294 K. */
	private static final double ANNEX_D_AMBIENT_C = 294.0 - 273.15;

	private final NaturalVentingCalculator calculator = new NaturalVentingCalculator();

	// NFPA 204 (2021) Table D.8: Q (kW), layer height above the floor (m), then D, L, mp, dT, M, corrected Av.
	private record AnnexDRow(double q, double layerHeight, double d, double l, double mp, double dT, double m, double correctedArea) {
	}

	private static final List<AnnexDRow> TABLE_D8 = List.of(
			new AnnexDRow(100_000, 3.0, 7.7, 15.6, 62.8, 557, 1.07, 17.6),
			new AnnexDRow(100_000, 6.0, 7.7, 15.6, 137.8, 253, 1.11, 53.8),
			new AnnexDRow(100_000, 7.3, 7.7, 15.6, 170.4, 205, 1.12, 89.2),
			new AnnexDRow(86_800, 3.0, 7.2, 14.9, 57.2, 531, 1.08, 16.1),
			new AnnexDRow(86_800, 6.0, 7.2, 14.9, 125.9, 241, 1.12, 49.7),
			new AnnexDRow(86_800, 7.3, 7.2, 14.9, 155.7, 195, 1.13, 82.6),
			new AnnexDRow(34_400, 3.0, 4.5, 10.7, 31.4, 383, 1.09, 8.6),
			new AnnexDRow(34_400, 6.0, 4.5, 10.7, 69.0, 174, 1.13, 28.3),
			new AnnexDRow(34_400, 7.3, 4.5, 10.7, 85.3, 141, 1.14, 47.8));

	private static VentZone annexDZone() {
		return new VentZone("Retail storage", 9.1, null, null, 73.0, 73.0, 0.61, 16, null, null, null, 18.3, 9.05, null, 1.5, null, null, null, null);
	}

	private static FireCase annexDFire(String name, double q, double layerHeight) {
		return new FireCase(name, "Retail storage", FireCaseKind.STEADY, q, null, null, null, null, null, null, 2136.0, null, 0.5, layerHeight);
	}

	private NaturalVentingInput annexDInput(List<FireCase> cases) {
		return new NaturalVentingInput(ANNEX_D_AMBIENT_C, 1.2, 1.0, null, null, List.of(annexDZone()), cases);
	}

	@Test
	void annexDSampleProblem_reproducesTableD8() {
		List<FireCase> cases = new java.util.ArrayList<>();
		for (int i = 0; i < TABLE_D8.size(); i++) {
			cases.add(annexDFire("Case " + (i + 1), TABLE_D8.get(i).q(), TABLE_D8.get(i).layerHeight()));
		}

		var result = calculator.calculate(annexDInput(cases));

		for (int i = 0; i < TABLE_D8.size(); i++) {
			AnnexDRow row = TABLE_D8.get(i);
			FireCaseResult c = result.fireCases().get(i);
			String label = "case " + (i + 1);
			assertEquals(row.d(), c.fireDiameter(), 0.06, label + " D");
			assertEquals(row.l(), c.meanFlameHeight(), 0.06, label + " L");
			assertEquals(row.mp(), c.plumeMassFlow(), row.mp() * 0.01, label + " mp");
			assertEquals(row.dT(), c.layerTemperatureRise(), 2.0, label + " dT");
			assertEquals(row.m(), c.inletCorrection(), 0.01, label + " M");
			assertEquals(row.correctedArea(), c.requiredVentArea(), row.correctedArea() * 0.01, label + " corrected Av");
		}
	}

	@Test
	void annexDCase1_flamesReachTheLayer_andMatchTheWorkedEquations() {
		var c = calculator.calculate(annexDInput(List.of(annexDFire("Case 1", 100_000, 3.0)))).fireCases().get(0);

		assertEquals(Regime.REACHES_LAYER, c.regime());
		assertEquals(2.5, c.layerHeightAboveFire(), 1e-9);
		assertEquals(6.1, c.layerDepth(), 1e-9);
		assertEquals(70_000, c.convectiveHeatReleaseRate(), 1e-6);
		// D.7b: aerodynamic vent area 10.04 m2; D.7c: 10.04 / 0.61 = 16.46 m2 geometric before the inlet term.
		assertEquals(10.04, c.aerodynamicVentArea(), 0.02);
		// D.5: flux at 458 K is 2.5 kW/m2; the layer here is at 851 K.
		assertEquals(29.7, c.floorRadiantFlux(), 0.5);
		// D.7h: 23,200 (H - d)^(5/2) = 229,265 kW.
		assertEquals(229_265, c.feasibleHeatReleaseRate(), 5);
	}

	@Test
	void plumeBelowTheLayer_usesTheVirtualOriginEquation() {
		// 1 MW, 0.5 m diameter: L = 0.235 * 1000^0.4 - 0.51 = 3.3 m, below a 6 m layer.
		var fire = new FireCase("Small", "Z", FireCaseKind.STEADY, 1000.0, null, null, null, null, 0.5, null, null, null, null, 6.0);
		var zone = new VentZone("Z", 8.0, null, null, null, null, 0.6, null, null, null, null, null, null, null, 1.5, null, null, null, null);

		var c = calculator.calculate(new NaturalVentingInput(20.0, null, null, null, null, List.of(zone), List.of(fire))).fireCases().get(0);

		assertEquals(Regime.BELOW_LAYER, c.regime());
		double qc = 700;
		double z0 = 0.083 * Math.pow(1000, 0.4) - 1.02 * 0.5;
		double dz = 6.0 - z0;
		double expected = 0.071 * Math.cbrt(qc) * Math.pow(dz, 5.0 / 3.0) * (1 + 0.027 * Math.pow(qc, 2.0 / 3.0) * Math.pow(dz, -5.0 / 3.0));
		assertEquals(expected, c.plumeMassFlow(), 1e-9);
		assertEquals(z0, c.virtualOrigin(), 1e-9);
	}

	@Test
	void growingFire_usesTheGrowthConstantInput_default1055() {
		FireCase growing = new FireCase("Growing", "Z", FireCaseKind.GROWING, null, 150.0, 90.0, 360.0, null, 3.0, null, null, null, null, 6.0);
		var zone = new VentZone("Z", 10.0, null, null, null, null, 0.6, null, null, null, null, null, null, null, 1.5, null, null, null, null);

		var byDefault = calculator.calculate(new NaturalVentingInput(20.0, null, null, null, null, List.of(zone), List.of(growing))).fireCases().get(0);
		var withOneThousand = calculator.calculate(new NaturalVentingInput(20.0, null, null, null, 1000.0, List.of(zone), List.of(growing))).fireCases().get(0);

		// Q = 1055 ((360 + 90) / 150)^2 = 9,495 kW; alpha_g = 1055 / 150^2 = 0.0469 kW/s2.
		assertEquals(9_495, byDefault.designHeatReleaseRate(), 1e-6);
		assertEquals(0.0469, byDefault.growthCoefficient(), 1e-4);
		assertEquals(9_000, withOneThousand.designHeatReleaseRate(), 1e-6);
		assertTrue(byDefault.checks().stream().anyMatch(ch -> ch.clause().equals("8.3.5")));
	}

	@Test
	void growingFire_limitedByItsFuel_isCappedAndFlaggedForConfirmation() {
		FireCase growing = new FireCase("Growing", "Z", FireCaseKind.GROWING, null, 75.0, 90.0, 360.0, 5000.0, 3.0, null, null, null, null, 6.0);
		var zone = new VentZone("Z", 10.0, null, null, null, null, 0.6, null, null, null, null, null, null, null, 1.5, null, null, null, null);

		var c = calculator.calculate(new NaturalVentingInput(20.0, null, null, null, null, List.of(zone), List.of(growing))).fireCases().get(0);

		assertTrue(c.heatReleaseRateCapped());
		assertEquals(5_000, c.designHeatReleaseRate(), 1e-9);
		assertTrue(c.checks().stream().anyMatch(ch -> ch.clause().equals("8.1.1") && ch.status() == CheckStatus.CONFIRM));
	}

	@Test
	void fireDiameter_fromAreaOrUnitHeatReleaseRate() {
		var zone = new VentZone("Z", 10.0, null, null, null, null, 0.6, null, null, null, null, null, null, null, 1.5, null, null, null, null);
		FireCase byArea = new FireCase("Area", "Z", FireCaseKind.STEADY, 2000.0, null, null, null, null, null, 12.0, null, null, null, 6.0);
		FireCase byFlux = new FireCase("Flux", "Z", FireCaseKind.STEADY, 2000.0, null, null, null, null, null, null, 500.0, null, null, 6.0);

		var result = calculator.calculate(new NaturalVentingInput(20.0, null, null, null, null, List.of(zone), List.of(byArea, byFlux)));

		assertEquals(Math.sqrt(4 * 12.0 / Math.PI), result.fireCases().get(0).fireDiameter(), 1e-9);
		assertEquals(Math.sqrt(4 * 2000.0 / (Math.PI * 500.0)), result.fireCases().get(1).fireDiameter(), 1e-9);
	}

	@Test
	void lineFire_withLayerBelowFourTimesItsLength_increasesThePlumeFlow() {
		var zone = new VentZone("Z", 10.0, null, null, null, null, 0.6, null, null, null, null, null, null, null, 1.5, null, null, null, null);
		FireCase point = new FireCase("Point", "Z", FireCaseKind.STEADY, 2000.0, null, null, null, null, 2.0, null, null, null, null, 6.0);
		FireCase line = new FireCase("Line", "Z", FireCaseKind.STEADY, 2000.0, null, null, null, null, 2.0, null, null, 3.0, null, 6.0);

		var result = calculator.calculate(new NaturalVentingInput(20.0, null, null, null, null, List.of(zone), List.of(point, line)));

		double factor = Math.pow(4 * 3.0 / 6.0, 2.0 / 3.0);
		assertEquals(factor, result.fireCases().get(1).lineFireFactor(), 1e-9);
		assertEquals(result.fireCases().get(0).plumeMassFlow() * factor, result.fireCases().get(1).plumeMassFlow(), 1e-9);
		assertEquals(1.0, result.fireCases().get(0).lineFireFactor(), 1e-9);
	}

	@Test
	void inletGivenAsArea_givesTheSameVentAreaAsTheEquivalentRatio() {
		var byRatio = calculator.calculate(annexDInput(List.of(annexDFire("Case 1", 100_000, 3.0)))).fireCases().get(0);
		double inletArea = 1.5 * byRatio.requiredVentArea();
		var zone = new VentZone("Retail storage", 9.1, null, null, 73.0, 73.0, 0.61, 16, null, null, null, 18.3, 9.05, null, null, inletArea, null, null, null);

		var byArea = calculator.calculate(new NaturalVentingInput(ANNEX_D_AMBIENT_C, 1.2, 1.0, null, null, List.of(zone),
				List.of(annexDFire("Case 1", 100_000, 3.0)))).fireCases().get(0);

		assertEquals(byRatio.requiredVentArea(), byArea.requiredVentArea(), 1e-6);
		assertNull(byArea.requiredInletArea());
	}

	@Test
	void inletTooSmallToPassThePlume_failsAndGivesNoVentArea() {
		var zone = new VentZone("Retail storage", 9.1, null, null, null, null, 0.61, null, null, null, null, null, null, null, null, 5.0, null, null, null);

		var c = calculator.calculate(new NaturalVentingInput(ANNEX_D_AMBIENT_C, 1.2, 1.0, null, null, List.of(zone),
				List.of(annexDFire("Case 1", 100_000, 3.0)))).fireCases().get(0);

		assertNull(c.requiredVentArea());
		assertNull(c.inletCorrection());
		assertTrue(c.checks().stream().anyMatch(ch -> ch.clause().equals("9.2.5.2") && ch.status() == CheckStatus.FAIL));
	}

	@Test
	void noInletGiven_opensTheInletCheck_andIgnoresTheInletTerm() {
		var zone = new VentZone("Retail storage", 9.1, null, null, null, null, 0.61, null, null, null, null, null, null, null, null, null, null, null, null);

		var c = calculator.calculate(new NaturalVentingInput(ANNEX_D_AMBIENT_C, 1.2, 1.0, null, null, List.of(zone),
				List.of(annexDFire("Case 1", 100_000, 3.0)))).fireCases().get(0);

		assertEquals(c.aerodynamicVentArea() / 0.61, c.requiredVentArea(), 1e-9);
		assertNull(c.inletCorrection());
		assertTrue(c.checks().stream().anyMatch(ch -> ch.clause().equals("9.2.5.2") && ch.status() == CheckStatus.OPEN));
	}

	@Test
	void ventAreaProvided_isComparedWithTheRequiredArea() {
		var required = calculator.calculate(annexDInput(List.of(annexDFire("Case 1", 100_000, 3.0)))).fireCases().get(0).requiredVentArea();
		FireCase fire = annexDFire("Case 1", 100_000, 3.0);

		CheckStatus enough = ventAreaStatus(required * 1.01, fire);
		CheckStatus short_ = ventAreaStatus(required * 0.99, fire);

		assertEquals(CheckStatus.PASS, enough);
		assertEquals(CheckStatus.FAIL, short_);
	}

	private CheckStatus ventAreaStatus(double provided, FireCase fire) {
		var zone = new VentZone("Retail storage", 9.1, null, null, null, null, 0.61, 16, provided, null, null, null, null, null, 1.5, null, null, null, null);
		var c = calculator.calculate(new NaturalVentingInput(ANNEX_D_AMBIENT_C, 1.2, 1.0, null, null, List.of(zone), List.of(fire))).fireCases().get(0);
		return c.checks().stream().filter(ch -> ch.clause().equals("9.2.5.1")).findFirst().orElseThrow().status();
	}

	@Test
	void annexDCase8_unitVentAreaIsWithinTwoDSquared() {
		// Annex D: 28.3 m2 over 16 vents is 1.77 m2 each, below 2 d^2 = 2 (3.1)^2 = 19.2 m2.
		var zone = new VentZone("Retail storage", 9.1, null, null, null, null, 0.61, 16, 28.3, null, null, null, null, null, 1.5, null, null, null, null);
		FireCase fire = annexDFire("Case 8", 34_400, 6.0);

		var c = calculator.calculate(new NaturalVentingInput(ANNEX_D_AMBIENT_C, 1.2, 1.0, null, null, List.of(zone), List.of(fire))).fireCases().get(0);

		var check = c.checks().stream().filter(ch -> ch.clause().equals("5.4.1.1")).findFirst().orElseThrow();
		assertEquals(CheckStatus.PASS, check.status());
		assertEquals(28.3 / 16, check.actual(), 1e-9);
		assertEquals(2 * 3.1 * 3.1, check.limit(), 1e-6);
	}

	@Test
	void layerTemperatureObjective_failsWhenTheRiseIsAboveIt() {
		var zone = new VentZone("Retail storage", 9.1, null, null, null, null, 0.61, null, null, null, null, null, null, null, 1.5, null, 164.0, 2.5, null);
		FireCase hot = annexDFire("Case 1", 100_000, 3.0);
		FireCase cooler = annexDFire("Case 9", 34_400, 7.3);

		var result = calculator.calculate(new NaturalVentingInput(ANNEX_D_AMBIENT_C, 1.2, 1.0, null, null, List.of(zone), List.of(hot, cooler)));

		assertEquals(CheckStatus.FAIL, status(result.fireCases().get(0), "4.1"));
		assertEquals(CheckStatus.PASS, status(result.fireCases().get(1), "4.1"));
		assertEquals(CheckStatus.FAIL, status(result.fireCases().get(0), "Annex D.5"));
		assertEquals(CheckStatus.PASS, status(result.fireCases().get(1), "Annex D.5"));
	}

	@Test
	void noLayerTemperatureObjective_isReportedOpen() {
		var c = calculator.calculate(annexDInput(List.of(annexDFire("Case 1", 100_000, 3.0)))).fireCases().get(0);

		assertEquals(CheckStatus.OPEN, status(c, "4.1"));
	}

	@Test
	void inletVelocity_isCheckedAgainstOneMetrePerSecond() {
		var zone = new VentZone("Retail storage", 9.1, null, null, null, null, 0.61, null, null, null, null, null, null, null, null, 100.0, null, null, null);
		FireCase fire = annexDFire("Case 8", 34_400, 6.0);

		var c = calculator.calculate(new NaturalVentingInput(ANNEX_D_AMBIENT_C, 1.2, 1.0, null, null, List.of(zone), List.of(fire))).fireCases().get(0);

		assertEquals(c.plumeMassFlow() / (1.2 * 100.0), c.inletVelocity(), 1e-9);
		assertEquals(c.plumeMassFlow() / 1.2, c.inletAreaForVelocityLimit(), 1e-9);
		assertEquals(CheckStatus.PASS, status(c, "6.6.3"));
	}

	@Test
	void curtainBottomAboveTheLayer_failsClause9_2_2_2() {
		// 9.1 m ceiling, 3 m curtains: bottom at 6.1 m. A layer held at 3 m is below the curtains.
		var zone = new VentZone("Retail storage", 9.1, null, 3.0, null, null, 0.61, null, null, null, null, null, null, null, 1.5, null, null, null, null);

		var c = calculator.calculate(new NaturalVentingInput(ANNEX_D_AMBIENT_C, 1.2, 1.0, null, null, List.of(zone),
				List.of(annexDFire("Case 1", 100_000, 3.0)))).fireCases().get(0);

		assertEquals(CheckStatus.FAIL, status(c, "9.2.2.2"));
	}

	@Test
	void zoneChecks_curtainDepthSpacingAndVentDistances() {
		// H = 9.1: curtain at least 1.82, area no more than 72.8 m each way, S <= 36.4, wall distance <= 25.48.
		var good = new VentZone("Good", 9.1, null, 2.0, 70.0, 60.0, 0.61, null, null, null, null, 18.3, 9.05, null, null, null, null, null, null);
		var bad = new VentZone("Bad", 9.1, null, 1.0, 80.0, 60.0, 0.61, null, null, null, null, 40.0, 30.0, null, null, null, null, null, null);
		var fires = List.of(
				new FireCase("A", "Good", FireCaseKind.STEADY, 1000.0, null, null, null, null, 1.0, null, null, null, null, 6.0),
				new FireCase("B", "Bad", FireCaseKind.STEADY, 1000.0, null, null, null, null, 1.0, null, null, null, null, 6.0));

		var result = calculator.calculate(new NaturalVentingInput(20.0, null, null, null, null, List.of(good, bad), fires));

		for (String clause : List.of("7.3.1", "7.4.1", "5.4.2", "5.4.3")) {
			assertEquals(CheckStatus.PASS, zoneStatus(result, 0, clause), "good " + clause);
			assertEquals(CheckStatus.FAIL, zoneStatus(result, 1, clause), "bad " + clause);
		}
		// Curtains 1.0 m is under 30 percent of 9.1 m, and the shorter side 60 m is above H, so 7.4.2 passes.
		assertEquals(CheckStatus.PASS, zoneStatus(result, 1, "7.4.2"));
	}

	@Test
	void zoneWithNothingGiven_reportsEachGeometryCheckOpen() {
		var zone = new VentZone("Z", 9.1, null, null, null, null, 0.61, null, null, null, null, null, null, null, null, null, null, null, null);

		FireCase fire = new FireCase("Case", "Z", FireCaseKind.STEADY, 1000.0, null, null, null, null, 1.0, null, null, null, null, 6.0);

		var result = calculator.calculate(new NaturalVentingInput(20.0, null, null, null, null, List.of(zone), List.of(fire)));

		for (var check : result.zones().get(0).checks()) {
			assertEquals(CheckStatus.OPEN, check.status(), check.clause());
		}
	}

	@Test
	void aisleWidth_isComparedWithThe8_2_3Minimum() {
		// Annex D: 3,250 kW needs 0.042 * sqrt(3250) = 2.39 m, and the aisle there is 2.4 m.
		var zone = new VentZone("Z", 9.1, null, null, null, null, 0.61, null, null, null, null, null, null, null, 1.5, null, null, null, 2.4);
		FireCase fire = new FireCase("Rack", "Z", FireCaseKind.STEADY, 3250.0, null, null, null, null, 3.0, null, null, null, null, 6.0);

		var c = calculator.calculate(new NaturalVentingInput(20.0, null, null, null, null, List.of(zone), List.of(fire))).fireCases().get(0);

		assertEquals(0.042 * Math.sqrt(3250), c.minimumAisleWidth(), 1e-9);
		assertEquals(CheckStatus.PASS, status(c, "8.2.3"));
	}

	@Test
	void ambientDensity_defaultsToTheIdealGasValue() {
		var input = new NaturalVentingInput(20.0, null, null, null, null, List.of(annexDZone()), List.of(annexDFire("Case 1", 1000.0, 3.0)));

		assertEquals(101.325 / (0.287 * 293.15), input.ambientDensity(), 1e-9);
		assertEquals(0.5, input.layerEnergyFraction(), 0.0);
		assertEquals(1055.0, input.growthConstant(), 0.0);
		assertEquals(1.0, input.specificHeat(), 0.0);
	}

	@Test
	void alteredK_needsConfirmation() {
		var c = calculator.calculate(new NaturalVentingInput(ANNEX_D_AMBIENT_C, 1.2, 1.0, 0.6, null, List.of(annexDZone()),
				List.of(annexDFire("Case 1", 100_000, 3.0)))).fireCases().get(0);

		assertEquals(CheckStatus.CONFIRM, status(c, "9.2.4.4"));
	}

	@Test
	void layerAtOrAboveTheVents_isRejected() {
		var zone = new VentZone("Z", 9.1, 6.0, null, null, null, 0.61, null, null, null, null, null, null, null, 1.5, null, null, null, null);
		FireCase fire = new FireCase("Case", "Z", FireCaseKind.STEADY, 1000.0, null, null, null, null, 1.0, null, null, null, null, 6.0);

		assertThrows(CalculationException.class,
				() -> calculator.calculate(new NaturalVentingInput(20.0, null, null, null, null, List.of(zone), List.of(fire))));
	}

	@Test
	void layerAboveTheCeiling_isRejected() {
		FireCase fire = new FireCase("Case", "Retail storage", FireCaseKind.STEADY, 1000.0, null, null, null, null, 1.0, null, null, null, null, 9.5);

		assertThrows(CalculationException.class, () -> calculator.calculate(annexDInput(List.of(fire))));
	}

	@Test
	void inputRules() {
		var zone = annexDZone();
		FireCase ok = annexDFire("Case", 1000.0, 3.0);

		assertThrows(CalculationException.class, () -> new NaturalVentingInput(20.0, null, null, null, null, List.of(), List.of(ok)));
		assertThrows(CalculationException.class, () -> new NaturalVentingInput(20.0, null, null, null, null, List.of(zone), List.of()));
		assertThrows(CalculationException.class, () -> new NaturalVentingInput(20.0, null, null, null, null, List.of(zone, zone), List.of(ok)));
		assertThrows(CalculationException.class,
				() -> new NaturalVentingInput(20.0, null, null, null, null, List.of(zone), List.of(ok, ok)));
		assertThrows(CalculationException.class, () -> new NaturalVentingInput(20.0, null, null, null, null, List.of(zone),
				List.of(new FireCase("Other", "Nowhere", FireCaseKind.STEADY, 1000.0, null, null, null, null, 1.0, null, null, null, null, 3.0))));
		assertThrows(CalculationException.class, () -> new NaturalVentingInput(20.0, null, null, 1.5, null, List.of(zone), List.of(ok)));
		assertThrows(CalculationException.class,
				() -> new FireCase("x", "Z", FireCaseKind.STEADY, 1000.0, null, null, null, null, 1.0, 5.0, null, null, null, 3.0));
		assertThrows(CalculationException.class,
				() -> new FireCase("x", "Z", FireCaseKind.STEADY, 1000.0, null, null, null, null, null, null, null, null, null, 3.0));
		assertThrows(CalculationException.class,
				() -> new FireCase("x", "Z", FireCaseKind.GROWING, null, 150.0, null, 360.0, null, 1.0, null, null, null, null, 3.0));
		assertThrows(CalculationException.class,
				() -> new FireCase("x", "Z", FireCaseKind.STEADY, 1000.0, null, null, null, null, 1.0, null, null, null, 3.0, 3.0));
		assertThrows(CalculationException.class,
				() -> new VentZone("Z", 9.1, null, null, null, null, 1.5, null, null, null, null, null, null, null, null, null, null, null, null));
		assertNotNull(calculator.calculate(new NaturalVentingInput(20.0, null, null, null, null, List.of(zone), List.of(ok))));
		assertFalse(calculator.calculate(new NaturalVentingInput(20.0, null, null, null, null, List.of(zone), List.of(ok))).fireCases().isEmpty());
	}

	private static CheckStatus status(FireCaseResult result, String clause) {
		return result.checks().stream().filter(ch -> ch.clause().equals(clause)).findFirst().orElseThrow().status();
	}

	private static CheckStatus zoneStatus(NaturalVentingResult result, int zoneIndex, String clause) {
		return result.zones().get(zoneIndex).checks().stream().filter(ch -> ch.clause().equals(clause)).findFirst().orElseThrow().status();
	}

}
