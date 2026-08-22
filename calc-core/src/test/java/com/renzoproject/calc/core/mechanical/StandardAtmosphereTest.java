package com.renzoproject.calc.core.mechanical;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StandardAtmosphereTest {

	private static Quantity<Length> metres(double value) {
		return Quantities.getQuantity(value, Units.METRE);
	}

	@Test
	void pressureAtAltitude_seaLevel_returnsStandardSeaLevelPressure() {
		Quantity<Pressure> pressure = StandardAtmosphere.pressureAtAltitude(metres(0.0));

		assertEquals(101325.0, pressure.to(Units.PASCAL).getValue().doubleValue(), 0.01);
	}

	@Test
	void pressureAtAltitude_500m_matchesPublishedIsaTableValue() {
		// Standard ISA table value at 500m is ~95461 Pa.
		Quantity<Pressure> pressure = StandardAtmosphere.pressureAtAltitude(metres(500.0));

		assertEquals(95461.0, pressure.to(Units.PASCAL).getValue().doubleValue(), 50.0);
	}

	@Test
	void pressureAtAltitude_nullAltitude_throws() {
		assertThrows(CalculationException.class, () -> StandardAtmosphere.pressureAtAltitude(null));
	}

}
