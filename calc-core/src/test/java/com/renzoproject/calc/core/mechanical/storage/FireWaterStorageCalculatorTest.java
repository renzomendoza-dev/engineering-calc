package com.renzoproject.calc.core.mechanical.storage;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.firepump.FirePumpUnits;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;

import javax.measure.Quantity;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FireWaterStorageCalculatorTest {

	private static final double DELTA = 1e-9;

	private final FireWaterDurationResolver durationResolver = new FakeFireWaterDurationResolver(Map.of(
			HazardClassification.LIGHT_HAZARD, new DurationRange(30.0, 30.0, 100.0),
			HazardClassification.ORDINARY_HAZARD, new DurationRange(60.0, 90.0, 250.0),
			HazardClassification.EXTRA_HAZARD, new DurationRange(90.0, 120.0, 500.0)));

	private final FireWaterStorageCalculator calculator = new FireWaterStorageCalculator(durationResolver);

	private static Quantity<VolumetricFlowRate> gpm(double value) {
		return Quantities.getQuantity(value, FirePumpUnits.GPM);
	}

	@Test
	void nullSelectedDuration_defaultsToConservativeMax() {
		FireWaterStorageInput input = new FireWaterStorageInput(gpm(500.0), HazardClassification.ORDINARY_HAZARD, null, 0.0);

		FireWaterStorageResult result = calculator.calculate(input);

		assertEquals(60.0, result.resolvedDurationMinutesMin(), DELTA);
		assertEquals(90.0, result.resolvedDurationMinutesMax(), DELTA);
		assertEquals(90.0, result.durationMinutesUsed(), DELTA);
		assertTrue(result.usedConservativeDefault());
		assertEquals(500.0 * 90.0, result.requiredStorageVolume().getValue().doubleValue(), DELTA);
	}

	@Test
	void inRangeSelectedDuration_isUsedDirectly() {
		FireWaterStorageInput input = new FireWaterStorageInput(gpm(500.0), HazardClassification.ORDINARY_HAZARD, 75.0, 0.0);

		FireWaterStorageResult result = calculator.calculate(input);

		assertEquals(75.0, result.durationMinutesUsed(), DELTA);
		assertFalse(result.usedConservativeDefault());
		assertEquals(500.0 * 75.0, result.requiredStorageVolume().getValue().doubleValue(), DELTA);
	}

	@Test
	void selectedDurationBelowRangeMinimum_throws() {
		FireWaterStorageInput input = new FireWaterStorageInput(gpm(500.0), HazardClassification.ORDINARY_HAZARD, 50.0, 0.0);

		assertThrows(CalculationException.class, () -> calculator.calculate(input));
	}

	@Test
	void selectedDurationAboveRangeMaximum_throws() {
		FireWaterStorageInput input = new FireWaterStorageInput(gpm(500.0), HazardClassification.ORDINARY_HAZARD, 100.0, 0.0);

		assertThrows(CalculationException.class, () -> calculator.calculate(input));
	}

	@Test
	void lightHazard_fixedThirtyMinutes_nullAndExplicitAgree() {
		FireWaterStorageInput nullSelected = new FireWaterStorageInput(gpm(100.0), HazardClassification.LIGHT_HAZARD, null, 0.0);
		FireWaterStorageInput explicitThirty = new FireWaterStorageInput(gpm(100.0), HazardClassification.LIGHT_HAZARD, 30.0, 0.0);

		FireWaterStorageResult nullResult = calculator.calculate(nullSelected);
		FireWaterStorageResult explicitResult = calculator.calculate(explicitThirty);

		assertEquals(30.0, nullResult.durationMinutesUsed(), DELTA);
		assertEquals(30.0, explicitResult.durationMinutesUsed(), DELTA);
		assertEquals(nullResult.requiredStorageVolume().getValue().doubleValue(),
				explicitResult.requiredStorageVolume().getValue().doubleValue(), DELTA);
	}

	@Test
	void lightHazard_anyOtherExplicitDuration_throwsSinceMinEqualsMax() {
		FireWaterStorageInput input = new FireWaterStorageInput(gpm(100.0), HazardClassification.LIGHT_HAZARD, 20.0, 0.0);

		assertThrows(CalculationException.class, () -> calculator.calculate(input));
	}

	@Test
	void safetyMargin_scalesVolumeProportionally() {
		FireWaterStorageInput input = new FireWaterStorageInput(gpm(500.0), HazardClassification.ORDINARY_HAZARD, null, 20.0);

		FireWaterStorageResult result = calculator.calculate(input);

		assertEquals(500.0 * 90.0 * 1.2, result.requiredStorageVolume().getValue().doubleValue(), DELTA);
	}

	@Test
	void nonPositiveRatedPumpFlow_throws() {
		assertThrows(CalculationException.class, () -> new FireWaterStorageInput(gpm(0.0), HazardClassification.LIGHT_HAZARD, null, 0.0));
		assertThrows(CalculationException.class, () -> new FireWaterStorageInput(gpm(-100.0), HazardClassification.LIGHT_HAZARD, null, 0.0));
	}

	@Test
	void nullRatedPumpFlow_throws() {
		assertThrows(CalculationException.class, () -> new FireWaterStorageInput(null, HazardClassification.LIGHT_HAZARD, null, 0.0));
	}

	@Test
	void missingHazardClassification_throws() {
		assertThrows(CalculationException.class, () -> new FireWaterStorageInput(gpm(500.0), null, null, 0.0));
	}

	@Test
	void negativeSafetyMargin_throws() {
		assertThrows(CalculationException.class, () -> new FireWaterStorageInput(gpm(500.0), HazardClassification.LIGHT_HAZARD, null, -1.0));
	}

}
