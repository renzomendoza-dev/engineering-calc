package com.renzoproject.calc.core.electrical.generatorset;

import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.LightingArea;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.MotorLoad;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.OtherLoad;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.OutputConductor;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.ReceptacleGroup;
import com.renzoproject.calc.core.electrical.reference.ConductorMaterial;
import com.renzoproject.calc.core.electrical.reference.InsulationType;
import com.renzoproject.calc.core.electrical.reference.MotorPhaseType;
import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratorSetSizingCalculatorTest {

	private static final double DELTA = 1e-6;
	private static final double SQRT_3 = Math.sqrt(3.0);

	private final GeneratorSetSizingCalculator calculator = new GeneratorSetSizingCalculator();

	private static LightingArea lighting(String occupancy, double areaM2) {
		return new LightingArea(null, occupancy, null, areaM2, null, false, true);
	}

	private static GeneratorSetSizingInput input(
			StandbyClass standbyClass,
			OptionalStandbyTransfer transfer,
			List<LightingArea> lighting,
			List<ReceptacleGroup> receptacles,
			List<OtherLoad> others,
			List<MotorLoad> motors,
			Double selectedKva,
			boolean designPreventsOverload,
			OutputConductor conductor) {
		return new GeneratorSetSizingInput(standbyClass, transfer, MotorPhaseType.THREE_PHASE, 230,
				lighting, receptacles, true, others, motors, 0.8, 0, selectedKva, designPreventsOverload, conductor);
	}

	private static GeneratorSetSizingInput lightingOnly(LightingArea area) {
		return input(StandbyClass.EMERGENCY, null, List.of(area), List.of(), List.of(), List.of(), null, false, null);
	}

	@Test
	void officeLoad_buildsDemandFromLightingReceptaclesAndAMotor() {
		// Office 1000 m2 x 28 VA/m2 = 28,000 VA at 100% (all others). 100 outlets in an office of
		// 1000 m2: the larger of 100 x 180 = 18,000 VA and 1000 x 11 = 11,000 VA, so 18,000 VA,
		// then Table 2.20.3.5: 10,000 at 100% + 8,000 at 50% = 14,000 VA. One 10 hp motor at
		// 230 V three-phase is 28 A: sqrt(3) x 230 x 28 VA.
		double motorVa = SQRT_3 * 230 * 28;
		GeneratorSetSizingInput input = input(StandbyClass.EMERGENCY, null,
				List.of(lighting("OFFICE_BUILDINGS", 1000)),
				List.of(new ReceptacleGroup(null, 100, 1000.0, false)),
				List.of(),
				List.of(new MotorLoad(null, "10", 230, 1)),
				null, false, null);

		GeneratorSetSizingResult result = calculator.calculate(input);

		assertEquals(3, result.loadRows().size());
		assertEquals(28_000, result.loadRows().get(0).demandVoltAmperes(), DELTA);
		assertEquals(18_000, result.loadRows().get(1).connectedVoltAmperes(), DELTA);
		assertEquals(14_000, result.loadRows().get(1).demandVoltAmperes(), DELTA);
		assertEquals(motorVa, result.loadRows().get(2).demandVoltAmperes(), DELTA);
		double demandVa = 28_000 + 14_000 + motorVa;
		assertEquals(demandVa / 1000, result.demandKva(), DELTA);
		assertEquals(demandVa / (SQRT_3 * 230), result.demandCurrentAmps(), DELTA);
		assertEquals(demandVa / 1000 * 0.8, result.requiredKw(), DELTA);
		// 53.2 kVA needs the 60 kVA size.
		assertEquals(60.0, result.recommendedStandardKva(), DELTA);
		assertEquals(60.0, result.selectedGeneratorKva(), DELTA);
		assertFalse(result.selectedIsUserEntered());
		assertTrue(result.selectedMeetsRequired());
	}

	@Test
	void generatorOutputConductor_is115PercentOfNameplateCurrent() {
		GeneratorSetSizingInput input = input(StandbyClass.EMERGENCY, null,
				List.of(lighting("OFFICE_BUILDINGS", 1000)), List.of(), List.of(), List.of(), 100.0, false,
				new OutputConductor(30.0, 3, 1, InsulationType.THHN, ConductorMaterial.COPPER, 75));

		GeneratorSetSizingResult result = calculator.calculate(input);

		double nameplate = 100_000 / (SQRT_3 * 230);
		assertEquals(nameplate, result.nameplateCurrentAmps(), DELTA);
		assertEquals(1.15, result.outputConductorFactor(), DELTA);
		assertEquals(nameplate * 1.15, result.minimumOutputConductorAmpacityAmps(), DELTA);
		assertTrue(result.selectedIsUserEntered());
		// The 115% must be applied exactly once: WireSizingCalculator adds no factor of its own.
		assertNotNull(result.outputConductor());
		assertEquals(nameplate * 1.15, result.outputConductor().requiredAmpacityAmps(), DELTA);
	}

	@Test
	void generatorOutputConductor_is100PercentWhereDesignPreventsOverload() {
		GeneratorSetSizingInput input = input(StandbyClass.EMERGENCY, null,
				List.of(lighting("OFFICE_BUILDINGS", 1000)), List.of(), List.of(), List.of(), 100.0, true, null);

		GeneratorSetSizingResult result = calculator.calculate(input);

		assertEquals(1.0, result.outputConductorFactor(), DELTA);
		assertEquals(result.nameplateCurrentAmps(), result.minimumOutputConductorAmpacityAmps(), DELTA);
		assertNull(result.outputConductor());
	}

	@Test
	void hospitalLighting_usesTheSteppedDemandFactor() {
		// 6000 m2 x 16 VA/m2 = 96,000 VA: 40% of the first 50,000 + 20% of the other 46,000.
		GeneratorSetSizingResult result = calculator.calculate(lightingOnly(lighting("HOSPITALS", 6000)));

		assertEquals(96_000, result.loadRows().get(0).connectedVoltAmperes(), DELTA);
		assertEquals(20_000 + 9_200, result.loadRows().get(0).demandVoltAmperes(), DELTA);
		assertEquals(29_200.0 / 96_000.0 * 100, result.loadRows().get(0).demandFactorPercent(), DELTA);
	}

	@Test
	void hospitalLighting_usedAtOnceTakesFullDemand() {
		LightingArea operatingRooms = new LightingArea("Operating rooms", "HOSPITALS", null, 500, null, true, true);

		GeneratorSetSizingResult result = calculator.calculate(lightingOnly(operatingRooms));

		assertEquals(8_000, result.loadRows().get(0).demandVoltAmperes(), DELTA);
		assertEquals(100.0, result.loadRows().get(0).demandFactorPercent(), DELTA);
	}

	@Test
	void areasOfTheSameOccupancy_shareOneSteppedFactor() {
		// Two 3,125 m2 hospital wards = 50,000 VA each, 100,000 VA together: 20,000 + 10,000.
		GeneratorSetSizingInput input = input(StandbyClass.EMERGENCY, null,
				List.of(lighting("HOSPITALS", 3125), lighting("HOSPITALS", 3125)),
				List.of(), List.of(), List.of(), null, false, null);

		GeneratorSetSizingResult result = calculator.calculate(input);

		assertEquals(30_000, result.demandKva() * 1000, DELTA);
	}

	@Test
	void hallsAndCorridors_takeTheirOwnUnitLoad() {
		LightingArea corridors = new LightingArea(null, "HOSPITALS", "HALLS_CORRIDORS_CLOSETS_STAIRWAYS", 1000, null, false, true);

		GeneratorSetSizingResult result = calculator.calculate(lightingOnly(corridors));

		assertEquals(4_000, result.loadRows().get(0).connectedVoltAmperes(), DELTA);
	}

	@Test
	void lightingUnitLoad_belowTheTableMinimumIsRejected() {
		LightingArea low = new LightingArea(null, "OFFICE_BUILDINGS", null, 100, 20.0, false, true);

		CalculationException error = assertThrows(CalculationException.class, () -> calculator.calculate(lightingOnly(low)));

		assertTrue(error.getMessage().contains("minimum of 28"));
	}

	@Test
	void lightingUnitLoad_aboveTheTableMinimumIsUsed() {
		LightingArea high = new LightingArea(null, "OFFICE_BUILDINGS", null, 100, 35.0, false, true);

		GeneratorSetSizingResult result = calculator.calculate(lightingOnly(high));

		assertEquals(3_500, result.loadRows().get(0).connectedVoltAmperes(), DELTA);
	}

	@Test
	void receptaclesWithoutDemandFactor_areTakenAtFullLoad() {
		GeneratorSetSizingInput base = input(StandbyClass.EMERGENCY, null, List.of(),
				List.of(new ReceptacleGroup(null, 100, null, false)), List.of(), List.of(), null, false, null);
		GeneratorSetSizingInput withoutFactor = new GeneratorSetSizingInput(
				base.standbyClass(), null, base.phaseType(), base.voltage(), base.lightingAreas(), base.receptacleGroups(),
				false, base.otherLoads(), base.motors(), 0.8, 0, null, false, null);

		assertEquals(14.0, calculator.calculate(base).demandKva(), DELTA);
		assertEquals(18.0, calculator.calculate(withoutFactor).demandKva(), DELTA);
	}

	@Test
	void loadFeeder_follows4302Point4() {
		// Two 10 hp motors at 230 V (28 A each): 125% of one + the other + 100% of a 10,000 VA
		// noncontinuous load + 125% of a 4,000 VA continuous load.
		double motorVa = SQRT_3 * 230 * 28;
		GeneratorSetSizingInput input = input(StandbyClass.OPTIONAL, OptionalStandbyTransfer.MANUAL,
				List.of(), List.of(),
				List.of(new OtherLoad("Pumps room", 10_000, 100, false), new OtherLoad("Control room", 4_000, 100, true)),
				List.of(new MotorLoad(null, "10", 230, 2)), null, false, null);

		GeneratorSetSizingResult result = calculator.calculate(input);

		double feederVa = 1.25 * motorVa + motorVa + 10_000 + 1.25 * 4_000;
		assertEquals(feederVa / (SQRT_3 * 230), result.loadFeederMinimumAmpacityAmps(), DELTA);
		assertNotNull(result.loadFeederBasis());
	}

	@Test
	void loadFeeder_isAbsentWithoutMotors() {
		GeneratorSetSizingResult result = calculator.calculate(lightingOnly(lighting("OFFICE_BUILDINGS", 100)));

		assertNull(result.loadFeederMinimumAmpacityAmps());
		assertNull(result.loadFeederBasis());
	}

	@Test
	void spareCapacity_isAddedToTheDemandLoad() {
		GeneratorSetSizingInput base = lightingOnly(lighting("OFFICE_BUILDINGS", 1000));
		GeneratorSetSizingInput withSpare = new GeneratorSetSizingInput(
				base.standbyClass(), null, base.phaseType(), base.voltage(), base.lightingAreas(), base.receptacleGroups(),
				true, base.otherLoads(), base.motors(), 0.8, 25, null, false, null);

		GeneratorSetSizingResult result = calculator.calculate(withSpare);

		assertEquals(28.0, result.demandKva(), DELTA);
		assertEquals(35.0, result.requiredKva(), DELTA);
		assertEquals(40.0, result.recommendedStandardKva(), DELTA);
	}

	@Test
	void selectedSetSmallerThanRequired_warnsAndFails() {
		GeneratorSetSizingInput input = input(StandbyClass.EMERGENCY, null,
				List.of(lighting("OFFICE_BUILDINGS", 1000)), List.of(), List.of(), List.of(), 20.0, false, null);

		GeneratorSetSizingResult result = calculator.calculate(input);

		assertFalse(result.selectedMeetsRequired());
		assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("smaller than")));
	}

	@Test
	void loadAboveTheLargestStandardSize_hasNoRecommendationAndSaysSo() {
		GeneratorSetSizingResult result = calculator.calculate(lightingOnly(lighting("OFFICE_BUILDINGS", 100_000)));

		assertNull(result.recommendedStandardKva());
		assertNull(result.selectedGeneratorKva());
		assertNull(result.nameplateCurrentAmps());
		assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("largest standard size")));
	}

	@Test
	void motorsPresent_warnThatStartingIsNotChecked() {
		GeneratorSetSizingInput input = input(StandbyClass.EMERGENCY, null, List.of(), List.of(), List.of(),
				List.of(new MotorLoad(null, "10", 230, 1)), null, false, null);

		GeneratorSetSizingResult result = calculator.calculate(input);

		assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("Motor starting is not checked")));
	}

	@Test
	void standbyClass_selectsTheChapter7Rules() {
		GeneratorSetSizingResult emergency = calculator.calculate(lightingOnly(lighting("OFFICE_BUILDINGS", 100)));
		GeneratorSetSizingInput optionalInput = input(StandbyClass.OPTIONAL, OptionalStandbyTransfer.AUTOMATIC_LOAD_MANAGEMENT,
				List.of(lighting("OFFICE_BUILDINGS", 100)), List.of(), List.of(), List.of(), null, false, null);
		GeneratorSetSizingResult optional = calculator.calculate(optionalInput);

		assertEquals("7.0.1.4(A)", emergency.capacityCitation());
		assertTrue(emergency.requirements().stream().anyMatch(item -> item.text().contains("10 seconds")));
		assertEquals("7.2.1.4(B)(2)(b)", optional.capacityCitation());
		assertTrue(optional.capacityBasis().contains("Article 2.20"));
	}

	@Test
	void optionalStandby_requiresATransferMode() {
		assertThrows(CalculationException.class, () -> input(StandbyClass.OPTIONAL, null,
				List.of(lighting("OFFICE_BUILDINGS", 100)), List.of(), List.of(), List.of(), null, false, null));
	}

	@Test
	void transferMode_isRejectedForEmergency() {
		assertThrows(CalculationException.class, () -> input(StandbyClass.EMERGENCY, OptionalStandbyTransfer.MANUAL,
				List.of(lighting("OFFICE_BUILDINGS", 100)), List.of(), List.of(), List.of(), null, false, null));
	}

	@Test
	void unknownOccupancy_isRejected() {
		assertThrows(CalculationException.class, () -> calculator.calculate(lightingOnly(lighting("MOON_BASE", 100))));
	}

	@Test
	void noLoad_isRejected() {
		assertThrows(CalculationException.class, () -> input(StandbyClass.EMERGENCY, null,
				List.of(), List.of(), List.of(), List.of(), null, false, null));
	}

	@Test
	void motorWithNoPublishedFullLoadCurrent_isRejected() {
		GeneratorSetSizingInput input = input(StandbyClass.EMERGENCY, null, List.of(), List.of(), List.of(),
				List.of(new MotorLoad(null, "10", 999, 1)), null, false, null);

		assertThrows(CalculationException.class, () -> calculator.calculate(input));
	}

	@Test
	void singlePhaseSet_usesSingleVoltageCurrentFormula() {
		GeneratorSetSizingInput input = new GeneratorSetSizingInput(StandbyClass.EMERGENCY, null, MotorPhaseType.SINGLE_PHASE, 230,
				List.of(lighting("OFFICE_BUILDINGS", 1000)), List.of(), true, List.of(), List.of(), 0.8, 0, 30.0, false, null);

		GeneratorSetSizingResult result = calculator.calculate(input);

		assertEquals(28_000.0 / 230, result.demandCurrentAmps(), DELTA);
		assertEquals(30_000.0 / 230, result.nameplateCurrentAmps(), DELTA);
	}

}
