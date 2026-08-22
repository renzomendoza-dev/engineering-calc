package com.renzoproject.calc.core.mechanical;

import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Volume;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoyleTankSizingTest {

	private static Quantity<Volume> litres(double value) {
		return Quantities.getQuantity(value, Units.LITRE);
	}

	private static Quantity<Pressure> pascal(double value) {
		return Quantities.getQuantity(value, Units.PASCAL);
	}

	@Test
	void requiredVolume_matchesHandCalculation() {
		// usableVolume=10L, low=200000 Pa, high=500000 Pa -> 10 / (1 - 200000/500000) = 10 / 0.6 = 16.6667 L
		Quantity<Volume> result = BoyleTankSizing.requiredVolume(litres(10.0), pascal(200000.0), pascal(500000.0));

		assertEquals(16.6667, result.to(Units.LITRE).getValue().doubleValue(), 0.001);
	}

	@Test
	void requiredVolume_narrowerPressureBand_producesLargerTank() {
		Quantity<Volume> wideband = BoyleTankSizing.requiredVolume(litres(10.0), pascal(200000.0), pascal(500000.0));
		Quantity<Volume> narrowband = BoyleTankSizing.requiredVolume(litres(10.0), pascal(400000.0), pascal(500000.0));

		assertTrue(narrowband.to(Units.LITRE).getValue().doubleValue() > wideband.to(Units.LITRE).getValue().doubleValue());
	}

}
