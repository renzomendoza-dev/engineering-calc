package com.renzoproject.calc.core.electrical.generatorset;

import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.DirectLoad;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.FirePump;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.PumpMotor;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.SupplyRun;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.LightingArea;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.MotorLoad;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.OtherLoad;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.OutputConductor;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.ReceptacleGroup;
import com.renzoproject.calc.core.electrical.reference.ConductorMaterial;
import com.renzoproject.calc.core.electrical.reference.ConduitMaterial;
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

	private static GeneratorSetSizingInput withStarting(
			MotorPhaseType phase, List<LightingArea> lighting, List<MotorLoad> motors, Double selectedKva, Double reactance, Double dip) {
		return new GeneratorSetSizingInput(StandbyClass.EMERGENCY, null, phase, 230, lighting, List.of(), true,
				List.of(), motors, 0.8, 0, selectedKva, false, null, reactance, dip);
	}

	@Test
	void motorStarting_setsTheSizeFromTheLockedRotorKvaAndTheDipLimit() {
		// 10 hp at 230 V three-phase: Table 4.30.14.5(B) locked-rotor current 162 A.
		double startingKva = SQRT_3 * 230 * 162 / 1000;
		GeneratorSetSizingInput input = withStarting(MotorPhaseType.THREE_PHASE, List.of(),
				List.of(new MotorLoad(null, "10", 230, 1)), null, 20.0, 15.0);

		GeneratorSetSizingResult result = calculator.calculate(input);

		// Set for a 15% dip at X"d 20%: X x S x (1/dip - 1).
		double required = 0.20 * startingKva * (1 / 0.15 - 1);
		assertEquals(1, result.startingSteps().size());
		assertEquals(startingKva, result.startingSteps().get(0).startingKva(), DELTA);
		assertEquals(required, result.startingRequiredKva(), DELTA);
		assertEquals(required, result.requiredKva(), DELTA);
		assertEquals("MOTOR_STARTING", result.governingCase());
		assertEquals(SQRT_3 * 230 * 28 / 1000, result.runningRequiredKva(), DELTA);
		// 73.1 kVA needs the 75 kVA size, which dips the voltage a little under 15%.
		assertEquals(75.0, result.recommendedStandardKva(), DELTA);
		double dip = 0.20 * startingKva / (75 + 0.20 * startingKva) * 100;
		assertEquals(dip, result.startingSteps().get(0).voltageDipPercentAtSelected(), DELTA);
		assertTrue(result.startingSteps().get(0).voltageDipPercentAtSelected() < 15.0);
		assertTrue(result.startingSteps().get(0).withinDipLimit());
		assertNotNull(result.startingBasis());
		assertFalse(result.warnings().stream().anyMatch(warning -> warning.contains("Motor starting is not checked")));
	}

	@Test
	void motorStarting_aSetEnteredBelowTheNeedDipsPastTheLimit() {
		GeneratorSetSizingInput input = withStarting(MotorPhaseType.THREE_PHASE, List.of(),
				List.of(new MotorLoad(null, "10", 230, 1)), 40.0, 20.0, 15.0);

		GeneratorSetSizingResult result = calculator.calculate(input);

		double startingKva = SQRT_3 * 230 * 162 / 1000;
		assertEquals(0.20 * startingKva / (40 + 0.20 * startingKva) * 100,
				result.startingSteps().get(0).voltageDipPercentAtSelected(), DELTA);
		assertFalse(result.startingSteps().get(0).withinDipLimit());
		assertFalse(result.selectedMeetsRequired());
	}

	@Test
	void motorStarting_runningLoadGovernsWhenItIsTheLargerNeed() {
		GeneratorSetSizingInput input = withStarting(MotorPhaseType.THREE_PHASE, List.of(lighting("OFFICE_BUILDINGS", 10_000)),
				List.of(new MotorLoad(null, "1", 230, 1)), null, 20.0, 15.0);

		GeneratorSetSizingResult result = calculator.calculate(input);

		assertEquals("RUNNING_LOAD", result.governingCase());
		assertEquals(result.runningRequiredKva(), result.requiredKva(), DELTA);
		assertTrue(result.startingRequiredKva() < result.runningRequiredKva());
	}

	@Test
	void motorStarting_motorsInTheSameStepStartTogether() {
		double eachKva = SQRT_3 * 230 * 162 / 1000;
		GeneratorSetSizingInput together = withStarting(MotorPhaseType.THREE_PHASE, List.of(),
				List.of(new MotorLoad(null, "10", 230, 2)), null, 20.0, 15.0);
		GeneratorSetSizingInput staggered = withStarting(MotorPhaseType.THREE_PHASE, List.of(),
				List.of(new MotorLoad("First", "10", 230, 1, 1, 100.0), new MotorLoad("Second", "10", 230, 1, 2, 100.0)),
				null, 20.0, 15.0);

		GeneratorSetSizingResult allAtOnce = calculator.calculate(together);
		GeneratorSetSizingResult oneByOne = calculator.calculate(staggered);

		assertEquals(2 * eachKva, allAtOnce.startingSteps().get(0).startingKva(), DELTA);
		assertEquals(2, oneByOne.startingSteps().size());
		assertEquals(eachKva, oneByOne.startingSteps().get(1).startingKva(), DELTA);
		// The second step starts with the first motor already running.
		assertEquals(0.0, oneByOne.startingSteps().get(0).runningKvaBefore(), DELTA);
		assertEquals(SQRT_3 * 230 * 28 / 1000, oneByOne.startingSteps().get(1).runningKvaBefore(), DELTA);
		assertTrue(oneByOne.requiredKva() < allAtOnce.requiredKva());
	}

	@Test
	void motorStarting_startingCurrentPercentScalesTheStartingKva() {
		double fullKva = SQRT_3 * 230 * 162 / 1000;
		GeneratorSetSizingInput input = withStarting(MotorPhaseType.THREE_PHASE, List.of(),
				List.of(new MotorLoad(null, "10", 230, 1, 1, 40.0)), null, 20.0, 15.0);

		GeneratorSetSizingResult result = calculator.calculate(input);

		assertEquals(fullKva * 0.4, result.startingSteps().get(0).startingKva(), DELTA);
	}

	@Test
	void motorStarting_singlePhaseUsesTheSinglePhaseTable() {
		// 1 hp at 230 V single-phase: Table 4.30.14.5(A) locked-rotor current 48 A.
		GeneratorSetSizingInput input = withStarting(MotorPhaseType.SINGLE_PHASE, List.of(),
				List.of(new MotorLoad(null, "1", 230, 1)), null, 20.0, 15.0);

		GeneratorSetSizingResult result = calculator.calculate(input);

		assertEquals(230 * 48 / 1000.0, result.startingSteps().get(0).startingKva(), DELTA);
	}

	@Test
	void motorStarting_aMotorWithNoPublishedLockedRotorCurrentIsLeftOutWithAWarning() {
		// 200 V single-phase has a full-load current but no locked-rotor current in Table 4.30.14.5(A).
		GeneratorSetSizingInput input = withStarting(MotorPhaseType.SINGLE_PHASE, List.of(),
				List.of(new MotorLoad("Pump", "1", 200, 1)), null, 20.0, 15.0);

		GeneratorSetSizingResult result = calculator.calculate(input);

		assertTrue(result.startingSteps().isEmpty());
		assertNull(result.startingRequiredKva());
		assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("Pump is left out") && warning.contains("NOT included")));
	}

	@Test
	void motorStarting_isSkippedWithoutReactanceAndDip() {
		GeneratorSetSizingInput input = withStarting(MotorPhaseType.THREE_PHASE, List.of(),
				List.of(new MotorLoad(null, "10", 230, 1)), null, null, null);

		GeneratorSetSizingResult result = calculator.calculate(input);

		assertTrue(result.startingSteps().isEmpty());
		assertNull(result.startingBasis());
		assertEquals("RUNNING_LOAD", result.governingCase());
		assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("Motor starting is not checked")));
	}

	@Test
	void motorStarting_needsBothReactanceAndDip() {
		assertThrows(CalculationException.class, () -> withStarting(MotorPhaseType.THREE_PHASE, List.of(),
				List.of(new MotorLoad(null, "10", 230, 1)), null, 20.0, null));
		assertThrows(CalculationException.class, () -> withStarting(MotorPhaseType.THREE_PHASE, List.of(),
				List.of(new MotorLoad(null, "10", 230, 1)), null, null, 15.0));
	}

	@Test
	void motorStarting_rejectsOutOfRangeInputs() {
		List<MotorLoad> motors = List.of(new MotorLoad(null, "10", 230, 1));
		assertThrows(CalculationException.class, () -> withStarting(MotorPhaseType.THREE_PHASE, List.of(), motors, null, 0.0, 15.0));
		assertThrows(CalculationException.class, () -> withStarting(MotorPhaseType.THREE_PHASE, List.of(), motors, null, 20.0, 100.0));
		assertThrows(CalculationException.class, () -> new MotorLoad(null, "10", 230, 1, 0, 100.0));
		assertThrows(CalculationException.class, () -> new MotorLoad(null, "10", 230, 1, 1, 120.0));
	}

	private static GeneratorSetSizingInput withDirect(
			List<MotorLoad> motors, List<DirectLoad> directLoads, Double reactance, Double dip) {
		return new GeneratorSetSizingInput(StandbyClass.EMERGENCY, null, MotorPhaseType.THREE_PHASE, 230,
				List.of(), List.of(), true, List.of(), motors, 0.8, 0, null, false, null, reactance, dip, directLoads);
	}

	private static DirectLoad building(String label, double amount, DirectLoadUnit unit, Double powerFactor) {
		return new DirectLoad(label, amount, unit, powerFactor, true);
	}

	private static PumpMotor byAmps(double fullLoadAmps, Double lockedRotorAmps) {
		return new PumpMotor(null, null, fullLoadAmps, lockedRotorAmps);
	}

	private static PumpMotor byHorsepower(String horsepower, int voltage) {
		return new PumpMotor(horsepower, voltage, null, null);
	}

	private static final SupplyRun SIZE_ONLY = new SupplyRun(
			30.0, 3, 1, InsulationType.THHN, ConductorMaterial.COPPER, 75, null, null, null, null, null);

	private static SupplyRun runOf(double lengthMeters) {
		return new SupplyRun(30.0, 3, 1, InsulationType.THHN, ConductorMaterial.COPPER, 75,
				lengthMeters, ConduitMaterial.PVC, 0.3, 0.85, 5.0);
	}

	private static GeneratorSetSizingInput withPump(
			int voltage, List<DirectLoad> directLoads, List<MotorLoad> motors, FirePump pump,
			Double selectedKva, Double reactance, Double dip) {
		return new GeneratorSetSizingInput(StandbyClass.EMERGENCY, null, MotorPhaseType.THREE_PHASE, voltage,
				List.of(), List.of(), true, List.of(), motors, 0.8, 0, selectedKva, false, null, reactance, dip,
				directLoads, pump);
	}

	private static FirePump pumpOf(PumpMotor motor, PumpMotor jockey, Double accessoryAmps, SupplyRun run) {
		return new FirePump(motor, jockey, accessoryAmps, 1, 100.0, run);
	}

	@Test
	void directBuildingLoad_inKw_goesThroughThePowerFactor() {
		GeneratorSetSizingResult result = calculator.calculate(
				withDirect(List.of(), List.of(building("Tower", 250, DirectLoadUnit.KW, 0.85)), null, null));

		assertEquals(250 / 0.85, result.demandKva(), DELTA);
		assertEquals(LoadKind.BUILDING, result.loadRows().get(0).kind());
		assertEquals("Tower", result.loadRows().get(0).label());
		assertEquals(100.0, result.loadRows().get(0).demandFactorPercent(), DELTA);
		assertEquals(300.0, result.recommendedStandardKva(), DELTA);
		assertTrue(result.loadRows().get(0).basis().contains("none is applied here"));
		assertTrue(result.loadRows().get(0).basis().contains("250 kW at 0.85 power factor"));
		assertTrue(result.warnings().isEmpty());
	}

	@Test
	void directBuildingLoad_inKvaAndAmps() {
		GeneratorSetSizingResult kva = calculator.calculate(
				withDirect(List.of(), List.of(building(null, 180, DirectLoadUnit.KVA, null)), null, null));
		GeneratorSetSizingResult amps = calculator.calculate(
				withDirect(List.of(), List.of(building(null, 400, DirectLoadUnit.AMPS, null)), null, null));

		assertEquals(180.0, kva.demandKva(), DELTA);
		assertEquals("Building load", kva.loadRows().get(0).label());
		assertEquals(SQRT_3 * 230 * 400 / 1000, amps.demandKva(), DELTA);
	}

	@Test
	void directLoads_areAddedToWhatIsBuiltUp() {
		GeneratorSetSizingInput input = new GeneratorSetSizingInput(StandbyClass.EMERGENCY, null, MotorPhaseType.THREE_PHASE, 230,
				List.of(lighting("OFFICE_BUILDINGS", 1000)), List.of(), true, List.of(), List.of(), 0.8, 0, null, false, null,
				null, null, List.of(building("Annex", 50, DirectLoadUnit.KVA, null)));

		GeneratorSetSizingResult result = calculator.calculate(input);

		assertEquals(78.0, result.demandKva(), DELTA);
		assertEquals(2, result.loadRows().size());
	}

	@Test
	void directLoad_inKwNeedsAPowerFactor() {
		assertThrows(CalculationException.class, () -> building("x", 100, DirectLoadUnit.KW, null));
		assertThrows(CalculationException.class, () -> building("x", 100, DirectLoadUnit.KW, 1.2));
		assertThrows(CalculationException.class, () -> building("x", 0, DirectLoadUnit.KVA, null));
	}

	@Test
	void aTotalLoadAloneIsEnoughToSize() {
		assertNotNull(calculator.calculate(withDirect(List.of(), List.of(building(null, 10, DirectLoadUnit.KVA, null)), null, null)));
	}

	@Test
	void firePump_byAmps_startsAgainstTheBuildingLoadAlreadyRunning() {
		// 20 kVA of building running, then a fire pump of 30 A (11.95 kVA) with 180 A locked rotor.
		double pumpRunningKva = SQRT_3 * 230 * 30 / 1000;
		double pumpStartingKva = SQRT_3 * 230 * 180 / 1000;
		GeneratorSetSizingInput input = withPump(230, List.of(building("Building", 20, DirectLoadUnit.KVA, null)), List.of(),
				pumpOf(byAmps(30, 180.0), null, null, null), null, 20.0, 15.0);

		GeneratorSetSizingResult result = calculator.calculate(input);

		assertEquals(20 + pumpRunningKva, result.demandKva(), DELTA);
		assertEquals(1, result.startingSteps().size());
		assertEquals("Fire pump", result.startingSteps().get(0).motors());
		assertEquals(pumpStartingKva, result.startingSteps().get(0).startingKva(), DELTA);
		// The pump is not running before its own step: only the building is.
		assertEquals(20.0, result.startingSteps().get(0).runningKvaBefore(), DELTA);
		// No run was given, so the generator has the whole 15% of Article 6.95.
		assertEquals(15.0, result.startingSteps().get(0).dipLimitPercent(), DELTA);
		assertEquals(0.20 * pumpStartingKva * (1 / 0.15 - 1), result.requiredKva(), DELTA);
		assertEquals("MOTOR_STARTING", result.governingCase());
		assertEquals(LoadKind.FIRE_PUMP, result.loadRows().get(1).kind());
		assertTrue(result.loadRows().get(1).basis().contains("6.95.1.3(D)(1)"));
		assertNotNull(result.firePump());
		assertEquals(30.0, result.firePump().pumpFullLoadAmps(), DELTA);
		assertEquals(180.0, result.firePump().pumpLockedRotorAmps(), DELTA);
		assertEquals(37.5, result.firePump().minimumConductorAmpacityAmps(), DELTA);
		assertTrue(result.firePump().conductorBasis().contains("6.95.1.6(B)(2)"));
		assertNull(result.firePump().conductor());
		assertTrue(result.firePump().requirements().stream().anyMatch(item -> item.citation().equals("6.95.1.4(B)(2)(b)")));
	}

	@Test
	void firePump_byHorsepower_readsThePecTables() {
		// 100 hp at 230 V three-phase: 248 A full load (Table 4.30.14.4), 1450 A locked rotor (4.30.14.5(B)).
		GeneratorSetSizingResult result = calculator.calculate(withPump(230, List.of(), List.of(),
				pumpOf(byHorsepower("100", 230), null, null, null), null, 20.0, 15.0));

		assertEquals(248.0, result.firePump().pumpFullLoadAmps(), DELTA);
		assertEquals(1450.0, result.firePump().pumpLockedRotorAmps(), DELTA);
		assertEquals(SQRT_3 * 230 * 248 / 1000, result.demandKva(), DELTA);
		assertTrue(result.loadRows().get(0).basis().contains("Table 4.30.14.4: 100 hp at 230 V draws 248 A"));
		assertEquals(SQRT_3 * 230 * 1450 / 1000, result.startingSteps().get(0).startingKva(), DELTA);
	}

	@Test
	void firePump_motorOnAnotherVoltage_isConvertedToTheSystemVoltage() {
		// A 10 hp motor at 460 V draws 14 A and 81 A locked rotor: twice that at a 230 V system.
		GeneratorSetSizingResult result = calculator.calculate(withPump(230, List.of(), List.of(),
				pumpOf(byHorsepower("10", 460), null, null, null), null, 20.0, 15.0));

		assertEquals(28.0, result.firePump().pumpFullLoadAmps(), DELTA);
		assertEquals(162.0, result.firePump().pumpLockedRotorAmps(), DELTA);
		assertEquals(SQRT_3 * 460 * 14 / 1000, result.demandKva(), DELTA);
	}

	@Test
	void firePump_withAJockeyPumpAndAccessories_followsTheSumOfSection6_95_1_6_B_1() {
		// 30 A pump + 3 hp jockey at 230 V (9.6 A): 125% of 39.6 A, plus 2 A of accessories at 100%.
		GeneratorSetSizingInput input = withPump(230, List.of(), List.of(),
				pumpOf(byAmps(30, 180.0), byHorsepower("3", 230), 2.0, null), null, 20.0, 15.0);

		GeneratorSetSizingResult result = calculator.calculate(input);

		assertEquals(1.25 * 39.6 + 2.0, result.firePump().minimumConductorAmpacityAmps(), DELTA);
		assertTrue(result.firePump().conductorBasis().contains("6.95.1.6(B)(1)"));
		assertEquals(3, result.loadRows().size());
		assertEquals("Pressure maintenance pump", result.loadRows().get(1).label());
		assertEquals("Fire pump accessories", result.loadRows().get(2).label());
		assertEquals(SQRT_3 * 230 * (30 + 9.6 + 2.0) / 1000, result.demandKva(), DELTA);
		// The jockey and the accessories are already running when the fire pump starts.
		assertEquals(SQRT_3 * 230 * (9.6 + 2.0) / 1000, result.startingSteps().get(0).runningKvaBefore(), DELTA);
	}

	@Test
	void firePump_conductorsAreSizedForTheAmpacityWhenNoLengthIsGiven() {
		GeneratorSetSizingResult result = calculator.calculate(withPump(230, List.of(), List.of(),
				pumpOf(byAmps(30, 180.0), null, null, SIZE_ONLY), null, 20.0, 15.0));

		assertNotNull(result.firePump().conductor());
		assertEquals(result.firePump().conductor().ampacitySizeLabel(), result.firePump().conductor().sizeLabel());
		assertFalse(result.firePump().conductor().upsizedForVoltageDrop());
		assertTrue(result.firePump().conductor().deratedAmpacityAmps() >= 37.5);
		assertNull(result.firePump().cableStartingDropPercent());
		assertNull(result.firePump().runningDropWithinLimit());
		assertEquals(15.0, result.startingSteps().get(0).dipLimitPercent(), DELTA);
	}

	@Test
	void firePump_aLongRunIsUpsizedForVoltageDrop_andTheCableTakesItsShareOfTheStartingDrop() {
		GeneratorSetSizingResult result = calculator.calculate(withPump(400, List.of(), List.of(),
				pumpOf(byAmps(40, 240.0), null, null, runOf(300)), null, 20.0, 15.0));

		var pump = result.firePump();
		assertNotNull(pump.conductor());
		assertTrue(pump.conductor().upsizedForVoltageDrop());
		assertTrue(pump.cableRunningDropPercent() <= 5.0);
		assertTrue(pump.runningDropWithinLimit());
		assertTrue(pump.cableStartingDropPercent() <= 5.0);
		// The generator gets what the cable leaves of the 15%.
		assertEquals(15.0 - pump.cableStartingDropPercent(), result.startingSteps().get(0).dipLimitPercent(), DELTA);
		assertEquals(pump.generatorStartingDropPercent() + pump.cableStartingDropPercent(), pump.controllerStartingDropPercent(), DELTA);
		assertTrue(pump.startingDropWithinLimit());
	}

	@Test
	void firePump_aCableThatTakesTheWholeStartingDropIsFlagged_andTheSetIsStillSizedSensibly() {
		// 100 hp at 400 V on 200 m of a single set: the cable alone drops more than the 15% allowed.
		GeneratorSetSizingResult result = calculator.calculate(withPump(400, List.of(), List.of(),
				pumpOf(byHorsepower("100", 400), null, null, runOf(200)), null, 20.0, 15.0));

		var pump = result.firePump();
		assertTrue(pump.cableStartingDropPercent() >= 14.0);
		assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("alone drops") && warning.contains("5% share")));
		assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("No conductor size keeps")));
		// Sized as if the cable took its 5% share, so the generator keeps 10%: a sane figure, not thousands of kVA.
		assertEquals(10.0, result.startingSteps().get(0).dipLimitPercent(), DELTA);
		assertNotNull(result.recommendedStandardKva());
		// ...while the result still shows the cable's real drop as over the limit.
		assertFalse(pump.startingDropWithinLimit());
	}

	@Test
	void firePump_aShortRunNeedsNoUpsizing() {
		GeneratorSetSizingResult result = calculator.calculate(withPump(400, List.of(), List.of(),
				pumpOf(byAmps(40, 240.0), null, null, runOf(10)), null, 20.0, 15.0));

		assertFalse(result.firePump().conductor().upsizedForVoltageDrop());
		assertTrue(result.firePump().cableRunningDropPercent() < 1.0);
	}

	@Test
	void firePump_aTighterGeneralDipLimitStillGovernsItsStep() {
		GeneratorSetSizingResult result = calculator.calculate(withPump(230, List.of(), List.of(),
				pumpOf(byAmps(30, 180.0), null, null, null), null, 20.0, 10.0));

		assertEquals(10.0, result.startingSteps().get(0).dipLimitPercent(), DELTA);
	}

	@Test
	void firePump_comparesTheSelectedSetWithThe225PercentOfTheGeneratorException() {
		// A 100 kVA set at 230 V is 251 A: well above 225% of a 30 A pump.
		GeneratorSetSizingResult large = calculator.calculate(withPump(230, List.of(), List.of(),
				pumpOf(byAmps(30, 180.0), null, null, null), 100.0, 20.0, 15.0));
		// A 15 kVA set at 230 V is 37.7 A: only 126% of it.
		GeneratorSetSizingResult small = calculator.calculate(withPump(230, List.of(), List.of(),
				pumpOf(byAmps(30, 180.0), null, null, null), 15.0, null, null));

		assertEquals(100_000 / (SQRT_3 * 230) / 30 * 100, large.firePump().generatorCurrentPercentOfPumpFla(), DELTA);
		assertTrue(large.firePump().generatorExceedsExceptionLevel());
		assertFalse(small.firePump().generatorExceedsExceptionLevel());
	}

	@Test
	void firePump_withoutALockedRotorCurrentIsLeftOutWithAWarning() {
		GeneratorSetSizingResult result = calculator.calculate(withPump(230, List.of(), List.of(),
				pumpOf(byAmps(30, null), null, null, null), null, 20.0, 15.0));

		assertTrue(result.startingSteps().isEmpty());
		assertNotNull(result.firePump());
		assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("fire pump is left out") && warning.contains("NOT included")));
		assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("Motor starting is not checked")));
	}

	@Test
	void firePump_sharesAStepWithMotorsAndStaysOutOfTheLoadFeeder() {
		List<MotorLoad> motors = List.of(new MotorLoad(null, "10", 230, 1));
		GeneratorSetSizingResult motorsOnly = calculator.calculate(withDirect(motors, List.of(), 20.0, 15.0));
		GeneratorSetSizingResult withPump = calculator.calculate(withPump(230, List.of(), motors,
				pumpOf(byAmps(30, 180.0), null, null, null), null, 20.0, 15.0));

		assertEquals(motorsOnly.loadFeederMinimumAmpacityAmps(), withPump.loadFeederMinimumAmpacityAmps(), DELTA);
		assertEquals(1, withPump.startingSteps().size());
		assertEquals(SQRT_3 * 230 * (162 + 180) / 1000, withPump.startingSteps().get(0).startingKva(), DELTA);
		assertEquals("10 hp motor, Fire pump", withPump.startingSteps().get(0).motors());
		assertEquals(15.0, withPump.startingSteps().get(0).dipLimitPercent(), DELTA);
	}

	@Test
	void aFirePumpAloneIsEnoughToSize_andNeedsNoReactanceForItsRunningLoad() {
		GeneratorSetSizingResult result = calculator.calculate(withPump(230, List.of(), List.of(),
				pumpOf(byAmps(30, 180.0), null, null, null), null, null, null));

		assertEquals(SQRT_3 * 230 * 30 / 1000, result.demandKva(), DELTA);
		assertTrue(result.startingSteps().isEmpty());
		assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("Motor starting is not checked")));
	}

	@Test
	void firePump_rejectsInconsistentPumpEntries() {
		assertThrows(CalculationException.class, () -> new PumpMotor(null, null, null, null));
		assertThrows(CalculationException.class, () -> new PumpMotor("10", 230, 28.0, null));
		assertThrows(CalculationException.class, () -> new PumpMotor("10", null, null, null));
		assertThrows(CalculationException.class, () -> new PumpMotor("10", 230, null, 162.0));
		assertThrows(CalculationException.class, () -> pumpOf(byAmps(30, 180.0), byAmps(5, 30.0), null, null));
		assertThrows(CalculationException.class, () -> new FirePump(byAmps(30, 180.0), null, null, 0, 100.0, null));
		assertThrows(CalculationException.class, () -> new FirePump(byAmps(30, 180.0), null, null, 1, 120.0, null));
	}

	@Test
	void firePump_aRunLengthNeedsTheDropInputs() {
		assertThrows(CalculationException.class, () -> new SupplyRun(
				30.0, 3, 1, InsulationType.THHN, ConductorMaterial.COPPER, 75, 50.0, null, 0.3, 0.85, 5.0));
		assertThrows(CalculationException.class, () -> new SupplyRun(
				30.0, 3, 1, InsulationType.THHN, ConductorMaterial.COPPER, 75, 50.0, ConduitMaterial.PVC, 0.3, 0.85, 15.0));
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
