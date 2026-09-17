package com.renzoproject.calc.core.mechanical.duct;

import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.MetricPrefix;
import javax.measure.Quantity;
import javax.measure.Unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DuctUnitsTest {

	private static final double DELTA = 1e-12;

	@Test
	void pascalPerMetre_isNotAPressure() {
		// The whole point of PressureGradient: a friction rate used to be a Quantity<Pressure>, so
		// converting it to kPa or psi "worked" and silently gave a meaningless number. Pa/m and Pa
		// have different dimensions, so Indriya must now refuse the conversion.
		assertFalse(DuctUnits.PASCAL_PER_METRE.isCompatible(Units.PASCAL));
	}

	@Test
	void pascalPerMetre_convertsToOtherGradientUnitsByDimension() {
		Quantity<PressureGradient> rate = Quantities.getQuantity(1.0, DuctUnits.PASCAL_PER_METRE);

		Unit<PressureGradient> kilopascalPerMetre =
				MetricPrefix.KILO(Units.PASCAL).divide(Units.METRE).asType(PressureGradient.class);
		Unit<PressureGradient> pascalPerMillimetre =
				Units.PASCAL.divide(MetricPrefix.MILLI(Units.METRE)).asType(PressureGradient.class);

		assertEquals(0.001, rate.to(kilopascalPerMetre).getValue().doubleValue(), DELTA);
		// Pa per mm is the smaller length, so the same gradient is 1000x smaller per mm.
		assertEquals(0.001, rate.to(pascalPerMillimetre).getValue().doubleValue(), DELTA);
	}

}
