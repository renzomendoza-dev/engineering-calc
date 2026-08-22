package com.renzoproject.calc.core.mechanical.storage;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link JsonPerCapitaConsumptionResolver} against the real {@code lpcd-consumption.json}
 * reference data.
 */
class JsonPerCapitaConsumptionResolverTest {

	private static final double DELTA = 1e-9;

	private final JsonPerCapitaConsumptionResolver resolver = new JsonPerCapitaConsumptionResolver();

	@Test
	void resolveLpcd_residentialDwelling_matchesPublishedValue() {
		assertEquals(150.0, resolver.resolveLpcd("RESIDENTIAL_DWELLING"), DELTA);
	}

	@Test
	void resolveLpcd_hospital_matchesPublishedValue() {
		assertEquals(300.0, resolver.resolveLpcd("HOSPITAL"), DELTA);
	}

	@Test
	void resolveLpcd_unknownType_throws() {
		assertThrows(CalculationException.class, () -> resolver.resolveLpcd("NOT_A_REAL_TYPE"));
	}

	@Test
	void resolveLpcd_null_throws() {
		assertThrows(CalculationException.class, () -> resolver.resolveLpcd(null));
	}

}
