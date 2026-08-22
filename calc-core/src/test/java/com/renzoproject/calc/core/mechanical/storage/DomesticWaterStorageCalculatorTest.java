package com.renzoproject.calc.core.mechanical.storage;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.PipeUnits;
import org.junit.jupiter.api.Test;
import tech.units.indriya.unit.Units;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomesticWaterStorageCalculatorTest {

	private static final double DELTA = 1e-6;

	private final PerCapitaConsumptionResolver perCapitaConsumptionResolver =
			new FakePerCapitaConsumptionResolver(Map.of("RESIDENTIAL_DWELLING", 150.0));
	private final FixtureUnitDemandResolver fixtureUnitDemandResolver =
			new FakeFixtureUnitDemandResolver(Map.of(SystemType.FLUSH_TANK, 100.0, SystemType.FLUSH_VALVE, 200.0));

	private final DomesticWaterStorageCalculator calculator =
			new DomesticWaterStorageCalculator(perCapitaConsumptionResolver, fixtureUnitDemandResolver);

	@Test
	void occupantLoad_twentyFourHourStorage_equalsDailyDemandExactly() {
		// dailyDemandLitres = 100 * 150 = 15000 L. With a 24-hour storage window and no safety
		// margin, the daily-average-as-hourly-rate flow rate integrated back over 24 hours
		// reproduces the daily demand exactly -- a clean regression check.
		DomesticWaterStorageInput input = new DomesticWaterStorageInput(
				DemandBasis.OCCUPANT_LOAD, 100, "RESIDENTIAL_DWELLING", null, null, 24.0, 0.0);

		DomesticWaterStorageResult result = calculator.calculate(input);

		assertEquals(15000.0 / 86400.0, result.resolvedDemandFlowRate().to(PipeUnits.LITRE_PER_SECOND).getValue().doubleValue(), DELTA);
		assertEquals(15000.0, result.requiredStorageVolume().to(Units.LITRE).getValue().doubleValue(), DELTA);
	}

	@Test
	void occupantLoad_safetyMargin_scalesVolumeProportionally() {
		DomesticWaterStorageInput input = new DomesticWaterStorageInput(
				DemandBasis.OCCUPANT_LOAD, 100, "RESIDENTIAL_DWELLING", null, null, 24.0, 50.0);

		DomesticWaterStorageResult result = calculator.calculate(input);

		assertEquals(22500.0, result.requiredStorageVolume().to(Units.LITRE).getValue().doubleValue(), DELTA);
	}

	@Test
	void fixtureUnit_flushTank_usesResolverGpmAndConvertsToSi() {
		// 60 GPM (round number) = 3.785411784 L/s exactly (1 gallon = 3.785411784 L).
		DomesticWaterStorageInput input = new DomesticWaterStorageInput(
				DemandBasis.FIXTURE_UNIT, null, null, 100.0, SystemType.FLUSH_TANK, 1.0, 0.0);
		FixtureUnitDemandResolver sixtyGpmResolver = new FakeFixtureUnitDemandResolver(
				Map.of(SystemType.FLUSH_TANK, 60.0, SystemType.FLUSH_VALVE, 60.0));
		DomesticWaterStorageCalculator sixtyGpmCalculator =
				new DomesticWaterStorageCalculator(perCapitaConsumptionResolver, sixtyGpmResolver);

		DomesticWaterStorageResult result = sixtyGpmCalculator.calculate(input);

		assertEquals(3.785411784, result.resolvedDemandFlowRate().to(PipeUnits.LITRE_PER_SECOND).getValue().doubleValue(), DELTA);
		assertEquals(3.785411784 * 3600.0, result.requiredStorageVolume().to(Units.LITRE).getValue().doubleValue(), 1e-4);
	}

	@Test
	void fixtureUnit_flushValve_forwardsSystemTypeToResolver() {
		DomesticWaterStorageInput input = new DomesticWaterStorageInput(
				DemandBasis.FIXTURE_UNIT, null, null, 50.0, SystemType.FLUSH_VALVE, 1.0, 0.0);

		DomesticWaterStorageResult flushTankResult = calculator.calculate(
				new DomesticWaterStorageInput(DemandBasis.FIXTURE_UNIT, null, null, 50.0, SystemType.FLUSH_TANK, 1.0, 0.0));
		DomesticWaterStorageResult flushValveResult = calculator.calculate(input);

		// FLUSH_TANK -> 100 GPM, FLUSH_VALVE -> 200 GPM per the fake resolver -- results must differ.
		assertEquals(2.0, flushValveResult.requiredStorageVolume().to(Units.LITRE).getValue().doubleValue()
				/ flushTankResult.requiredStorageVolume().to(Units.LITRE).getValue().doubleValue(), DELTA);
	}

	@Test
	void nonPositiveStorageDurationHours_throws() {
		assertThrows(CalculationException.class, () -> new DomesticWaterStorageInput(
				DemandBasis.OCCUPANT_LOAD, 100, "RESIDENTIAL_DWELLING", null, null, 0.0, 0.0));
	}

	@Test
	void occupantLoad_missingOccupantCount_throws() {
		assertThrows(CalculationException.class, () -> new DomesticWaterStorageInput(
				DemandBasis.OCCUPANT_LOAD, null, "RESIDENTIAL_DWELLING", null, null, 24.0, 0.0));
	}

	@Test
	void occupantLoad_missingOccupancyType_throws() {
		assertThrows(CalculationException.class, () -> new DomesticWaterStorageInput(
				DemandBasis.OCCUPANT_LOAD, 100, null, null, null, 24.0, 0.0));
	}

	@Test
	void fixtureUnit_missingTotalFixtureUnits_throws() {
		assertThrows(CalculationException.class, () -> new DomesticWaterStorageInput(
				DemandBasis.FIXTURE_UNIT, null, null, null, SystemType.FLUSH_TANK, 1.0, 0.0));
	}

	@Test
	void fixtureUnit_missingSystemType_throws() {
		assertThrows(CalculationException.class, () -> new DomesticWaterStorageInput(
				DemandBasis.FIXTURE_UNIT, null, null, 50.0, null, 1.0, 0.0));
	}

	@Test
	void occupantLoad_unknownOccupancyType_propagatesResolverException() {
		DomesticWaterStorageInput input = new DomesticWaterStorageInput(
				DemandBasis.OCCUPANT_LOAD, 100, "NOT_A_REAL_TYPE", null, null, 24.0, 0.0);

		assertThrows(CalculationException.class, () -> calculator.calculate(input));
	}

	@Test
	void fixtureUnit_resolverException_propagates() {
		FixtureUnitDemandResolver throwingResolver = new FixtureUnitDemandResolver() {
			@Override
			public double resolveGpm(double totalWsfu, SystemType systemType) {
				throw new CalculationException("out of range");
			}

			@Override
			public java.util.List<WsfuDemandRow> allEntries() {
				throw new UnsupportedOperationException("Not needed by this test");
			}
		};
		DomesticWaterStorageCalculator throwingCalculator =
				new DomesticWaterStorageCalculator(perCapitaConsumptionResolver, throwingResolver);
		DomesticWaterStorageInput input = new DomesticWaterStorageInput(
				DemandBasis.FIXTURE_UNIT, null, null, 50.0, SystemType.FLUSH_TANK, 1.0, 0.0);

		assertThrows(CalculationException.class, () -> throwingCalculator.calculate(input));
	}

}
