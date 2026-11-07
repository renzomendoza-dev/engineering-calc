package com.renzoproject.calc.core.smokecontrol.naturalventing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.smokecontrol.naturalventing.NaturalVentingResult.FireCaseResult;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The supplementary methods, not NFPA 204: the image-fire screening for a fire against a wall or in a
 * corner and the time-stepping check of the smoke layer boundary. Both are checked against the IN1GO PPCI
 * XDOCK supporting calculations (the two warehouses, cartons 3.0 m high at 1,133 kW/m2, layer held at 7.0 m,
 * ambient 40 C, density 1.127, cp 1.00 and K = 1.0 as that document uses).
 */
class NaturalVentingSupplementaryTest {

	private final NaturalVentingCalculator calculator = new NaturalVentingCalculator();

	private static final double UNIT_HEAT_RELEASE_RATE = 1700.0 * 3.0 / 4.5;

	/** A gable warehouse, ridge 12 m, eave 8 m, 60 m long, with the 144 m2 raised ridge monitor at Cd,v 0.25 and fixed louvers. */
	private static VentZone warehouse(String name, double width, double inletGrossArea) {
		return new VentZone(name, 12.0, 12.75, null, 60.0, width, 0.25, null, 144.0, null, null, null, null, 0.25, null, inletGrossArea,
				null, null, null, 8.0);
	}

	private static FireCase ultrafast(String zone) {
		return new FireCase("Ultrafast", zone, FireCaseKind.GROWING, null, 75.0, 90.0, null, null, 360.0, null, null, null,
				UNIT_HEAT_RELEASE_RATE, null, 0.0, 7.0);
	}

	private static FireCase fast(String zone) {
		return new FireCase("Fast", zone, FireCaseKind.GROWING, null, 150.0, 90.0, null, null, 360.0, null, null, null,
				UNIT_HEAT_RELEASE_RATE, null, 0.0, 7.0);
	}

	private NaturalVentingInput input(VentZone zone, FireCase fireCase) {
		return new NaturalVentingInput(40.0, 1.127, 1.0, 1.0, null, List.of(zone), List.of(fireCase));
	}

	private FireCaseResult run(VentZone zone, FireCase fireCase) {
		return calculator.calculate(input(zone, fireCase)).fireCases().get(0);
	}

	private static VentingCheck check(FireCaseResult result, String clause, String descriptionPart) {
		return result.checks().stream().filter(c -> c.clause().equals(clause) && c.description().contains(descriptionPart)).findFirst().orElseThrow();
	}

	// --- image fire --------------------------------------------------------------------------------------------

	private FireCaseResult steady(double q) {
		var zone = new VentZone("Z", 12.0, 12.75, null, 60.0, 20.0, 0.25, null, 144.0, null, null, null, null, 0.25, null, 128.0, null, null, null, null);
		var fire = new FireCase("Steady", "Z", FireCaseKind.STEADY, q, null, null, null, null, null, null, null, null, UNIT_HEAT_RELEASE_RATE, null, 0.0, 7.0);
		return run(zone, fire);
	}

	private static double celsius(double kelvin) {
		return kelvin - 273.15;
	}

	@Test
	void imageFire_reproducesTheDocumentsWallAndCornerLayerTemperatures() {
		// 5.0 MW: wall 182 C, corner 232 C. 9.5 MW: 228 and 277. 38.0 MW: 337 and 409 (K = 1.0).
		var five = steady(5_000).imageFire();
		var nineAndAHalf = steady(9_497).imageFire();
		var thirtyEight = steady(37_989).imageFire();

		assertEquals(182.0, celsius(five.wall().layerTemperature()), 0.6);
		assertEquals(232.0, celsius(five.corner().layerTemperature()), 0.6);
		assertEquals(228.0, celsius(nineAndAHalf.wall().layerTemperature()), 0.6);
		assertEquals(277.0, celsius(nineAndAHalf.corner().layerTemperature()), 0.6);
		assertEquals(337.0, celsius(thirtyEight.wall().layerTemperature()), 0.6);
		assertEquals(409.0, celsius(thirtyEight.corner().layerTemperature()), 0.6);
	}

	@Test
	void imageFire_treatsTheFireAsTwoOrFourTimesItsSize_andDividesThePlumeFlowByIt() {
		var result = steady(5_000);
		var image = result.imageFire();

		assertEquals(2.0, image.wall().multiplier(), 1e-9);
		assertEquals(10_000.0, image.wall().equivalentHeatReleaseRate(), 1e-9);
		assertEquals(4.0, image.corner().multiplier(), 1e-9);
		assertEquals(20_000.0, image.corner().equivalentHeatReleaseRate(), 1e-9);
		// dT = K Qc / (cp mp): the actual convective heat release rate over the divided plume flow.
		assertEquals(1.0 * result.convectiveHeatReleaseRate() / image.wall().plumeMassFlow(), image.wall().layerTemperatureRise(), 1e-9);
		assertTrue(image.corner().layerTemperatureRise() > image.wall().layerTemperatureRise());
		assertTrue(image.wall().layerTemperatureRise() > result.layerTemperatureRise());
	}

	@Test
	void imageFire_isReportedAsSupplementaryChecks_againstTheTemperatureObjectiveWhenOneIsGiven() {
		var open = steady(5_000);
		var wall = check(open, "Image fire (wall)", "temperature rise");
		assertEquals(CheckBasis.SUPPLEMENTARY, wall.basis());
		assertEquals(CheckStatus.INFO, wall.status());
		assertEquals(open.imageFire().wall().layerTemperatureRise(), wall.actual(), 1e-9);

		var zone = new VentZone("Z", 12.0, 12.75, null, 60.0, 20.0, 0.25, null, 144.0, null, null, null, null, 0.25, null, 128.0, 160.0, null, null, null);
		var fire = new FireCase("Steady", "Z", FireCaseKind.STEADY, 5_000.0, null, null, null, null, null, null, null, null, UNIT_HEAT_RELEASE_RATE, null, 0.0, 7.0);
		var withObjective = run(zone, fire);
		// With K = 1.0 the wall fire's rise is 142 K and the corner's 192 K, against an objective of 160 K.
		assertEquals(CheckStatus.PASS, check(withObjective, "Image fire (wall)", "within the design objective").status());
		assertEquals(CheckStatus.FAIL, check(withObjective, "Image fire (corner)", "within the design objective").status());
		assertEquals(160.0, check(withObjective, "Image fire (corner)", "within the design objective").limit(), 1e-9);
	}

	// --- time-stepping -----------------------------------------------------------------------------------------

	@Test
	void timeStepping_reproducesTheDocumentsWarehouse1UltrafastCase() {
		var result = run(warehouse("W1", 20.0, 128.0), ultrafast("W1"));
		var step = result.transientInterface();

		assertNotNull(step);
		assertEquals(450.0, step.endTime(), 1e-9);
		// "10.2 m at 450 s, reaching 7.0 m at 646 s; without any venting 7.0 m at 174 s".
		assertEquals(10.2, step.interfaceHeightAtEnd(), 0.05);
		assertEquals(646.0, step.timeToDesignHeight(), 1.5);
		assertEquals(174.0, step.timeToDesignHeightWithoutVents(), 1.5);
	}

	@Test
	void timeStepping_reproducesTheDocumentsWarehouse2UltrafastCase() {
		var step = run(warehouse("W2", 30.0, 160.0), ultrafast("W2")).transientInterface();

		// "10.6 m at 450 s, reaching 7.0 m at 696 s; without any venting 7.0 m at 210 s".
		assertEquals(10.6, step.interfaceHeightAtEnd(), 0.05);
		assertEquals(696.0, step.timeToDesignHeight(), 1.5);
		assertEquals(210.0, step.timeToDesignHeightWithoutVents(), 1.5);
	}

	@Test
	void timeStepping_holdsTheLayerAtTheRidgeForTheFastFire() {
		var result = run(warehouse("W1", 20.0, 128.0), fast("W1"));

		assertEquals(12.0, result.transientInterface().interfaceHeightAtEnd(), 1e-6);
		assertEquals(CheckStatus.PASS, check(result, "Time-stepping", "at or above the design height").status());
	}

	@Test
	void timeStepping_isACheckAgainstTheDesignHeight_thatFailsWhenTheLayerHasFallenBelowIt() {
		var passing = run(warehouse("W1", 20.0, 128.0), ultrafast("W1"));
		var heightCheck = check(passing, "Time-stepping", "at or above the design height");
		assertEquals(CheckStatus.PASS, heightCheck.status());
		assertEquals(passing.transientInterface().interfaceHeightAtEnd(), heightCheck.actual(), 1e-9);
		assertEquals(7.0, heightCheck.limit(), 1e-9);
		assertEquals(CheckBasis.SUPPLEMENTARY, heightCheck.basis());

		// The same fire with no vents area to speak of: the layer falls below 7 m long before the end of the interval.
		var poorlyVented = new VentZone("W1", 12.0, 12.75, null, 60.0, 20.0, 0.25, null, 4.0, null, null, null, null, 0.25, null, 128.0, null, null, null, 8.0);
		var failing = run(poorlyVented, ultrafast("W1"));
		assertEquals(CheckStatus.FAIL, check(failing, "Time-stepping", "at or above the design height").status());
		assertTrue(failing.transientInterface().interfaceHeightAtEnd() < 7.0);
		assertTrue(failing.transientInterface().timeToDesignHeight() < 450.0);
	}

	@Test
	void timeStepping_reportsTheTimesToTheDesignHeight_orNoneWithinTheHorizon() {
		var ultra = run(warehouse("W1", 20.0, 128.0), ultrafast("W1"));
		assertEquals(CheckStatus.INFO, check(ultra, "Time-stepping", "with the vents working").status());
		assertEquals(ultra.transientInterface().timeToDesignHeight(), check(ultra, "Time-stepping", "with the vents working").actual(), 1e-9);
		assertEquals(ultra.transientInterface().timeToDesignHeightWithoutVents(), check(ultra, "Time-stepping", "no venting").actual(), 1e-9);

		// The fast fire, if it kept growing, would bring the layer to 7 m long after the design interval.
		var fastResult = run(warehouse("W1", 20.0, 128.0), fast("W1"));
		assertEquals(1254.0, fastResult.transientInterface().timeToDesignHeight(), 2.0);

		// A 5.0 MW fire is held there and the vents pass all of it: the layer never falls to 7 m, which is said so.
		var capped = new FireCase("Capped", "W1", FireCaseKind.GROWING, null, 75.0, 90.0, null, null, 360.0, 5_000.0, null, null,
				UNIT_HEAT_RELEASE_RATE, null, 0.0, 7.0);
		var cappedResult = run(warehouse("W1", 20.0, 128.0), capped);
		var withVents = check(cappedResult, "Time-stepping", "with the vents working");
		assertNull(cappedResult.transientInterface().timeToDesignHeight());
		assertNull(withVents.actual());
		assertTrue(withVents.note().contains("Not reached"));
		assertNotNull(cappedResult.transientInterface().timeToDesignHeightWithoutVents());
	}

	@Test
	void timeStepping_isOpenWithoutTheZoneGeometry_andAbsentForASteadyFire() {
		var noGeometry = new VentZone("Z", 12.0, 12.75, null, null, null, 0.25, null, 144.0, null, null, null, null, 0.25, null, 128.0, null, null, null, 8.0);
		var open = run(noGeometry, ultrafast("Z"));
		assertNull(open.transientInterface());
		assertEquals(CheckStatus.OPEN, check(open, "Time-stepping", "stepped as the fire grows").status());

		var noVentArea = new VentZone("Z", 12.0, 12.75, null, 60.0, 20.0, 0.25, null, null, null, null, null, null, 0.25, null, 128.0, null, null, null, 8.0);
		assertNull(run(noVentArea, ultrafast("Z")).transientInterface());

		var steadyResult = steady(5_000);
		assertNull(steadyResult.transientInterface());
		assertTrue(steadyResult.checks().stream().noneMatch(c -> c.clause().equals("Time-stepping")));
	}

	@Test
	void timeStepping_takesTheInletFromTheRatioWhenNoAreaIsGiven_andACapHoldsTheFireToIt() {
		var ratio = new VentZone("W1", 12.0, 12.75, null, 60.0, 20.0, 0.25, null, 144.0, null, null, null, null, 0.25, 1.0, null, null, null, null, 8.0);
		var byArea = warehouse("W1", 20.0, 144.0);
		// A 1.0 ratio gives an inlet of the vent's own area, 144 m2, which is the same as giving 144 m2.
		assertEquals(run(byArea, ultrafast("W1")).transientInterface().interfaceHeightAtEnd(),
				run(ratio, ultrafast("W1")).transientInterface().interfaceHeightAtEnd(), 1e-9);

		var capped = new FireCase("Capped", "W1", FireCaseKind.GROWING, null, 75.0, 90.0, null, null, 360.0, 5_000.0, null, null,
				UNIT_HEAT_RELEASE_RATE, null, 0.0, 7.0);
		// The document's Case 1: a 5.0 MW fire is held at the ridge.
		assertEquals(12.0, run(warehouse("W1", 20.0, 128.0), capped).transientInterface().interfaceHeightAtEnd(), 1e-6);
	}

	@Test
	void reservoir_giveTheBoundaryHeightForASmokeVolume_flatAndGable() {
		var gable = new TransientInterfaceModel.Reservoir(20.0, 60.0, 12.0, 8.0);
		// Nothing under the ridge, the volume above the eave is the triangular prism 20 x 60 x 4 / 2 = 2,400 m3, and the rest is below the eave.
		assertEquals(12.0, gable.heightFor(0), 1e-9);
		assertEquals(8.0, gable.heightFor(2_400), 1e-9);
		assertEquals(7.0, gable.heightFor(2_400 + 1_200), 1e-9);
		assertEquals(0.0, gable.heightFor(1e9), 1e-9);
		// Halfway down the roof the smoke fills a quarter of the prism: area scales with the square of the depth.
		assertEquals(12.0 - 2.0, gable.heightFor(600), 1e-9);

		var flat = new TransientInterfaceModel.Reservoir(20.0, 60.0, 9.0, 9.0);
		assertEquals(6.0, flat.heightFor(20.0 * 60.0 * 3.0), 1e-9);
	}

	// --- zone input ---------------------------------------------------------------------------------------------

	@Test
	void ventCentreAboveTheCeiling_isAllowedForARaisedMonitor_andTheEaveMustBeUnderTheRidge() {
		var monitor = warehouse("W1", 20.0, 128.0);
		assertEquals(12.75, monitor.ventCentreHeight(), 1e-9);
		assertEquals(5.75, run(monitor, ultrafast("W1")).layerDepth(), 1e-9);

		assertThrows(CalculationException.class,
				() -> new VentZone("Z", 12.0, 12.75, null, 60.0, 20.0, 0.25, null, 144.0, null, null, null, null, 0.25, null, 128.0, null, null, null, 13.0));
		assertThrows(CalculationException.class,
				() -> new VentZone("Z", 12.0, 12.75, null, 60.0, 20.0, 0.25, null, 144.0, null, null, null, null, 0.25, null, 128.0, null, null, null, 0.0));
		assertThrows(CalculationException.class,
				() -> new VentZone("Z", 12.0, 0.0, null, 60.0, 20.0, 0.25, null, 144.0, null, null, null, null, 0.25, null, 128.0, null, null, null, null));
	}

}
