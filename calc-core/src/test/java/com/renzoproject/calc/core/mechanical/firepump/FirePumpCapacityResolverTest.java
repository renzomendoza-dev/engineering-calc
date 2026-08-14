package com.renzoproject.calc.core.mechanical.firepump;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link JsonFirePumpCapacityResolver} against the real {@code standard-capacities.json}
 * reference data, to confirm the loader itself works correctly.
 */
class FirePumpCapacityResolverTest {

	private static final double DELTA = 1e-9;

	private final JsonFirePumpCapacityResolver resolver = new JsonFirePumpCapacityResolver();

	@Test
	void resolveNextStandardCapacity_roundsUpToSmallestSufficientCapacity() {
		// Published capacities include ...300, 400... ; 320 GPM should round up to 400.
		StandardPumpRating rating = resolver.resolveNextStandardCapacity(Quantities.getQuantity(320.0, FirePumpUnits.GPM));

		assertEquals(400.0, rating.standardFlow().to(FirePumpUnits.GPM).getValue().doubleValue(), DELTA);
	}

	@Test
	void resolveNextStandardCapacity_exactMatchIsSufficient() {
		StandardPumpRating rating = resolver.resolveNextStandardCapacity(Quantities.getQuantity(5000.0, FirePumpUnits.GPM));

		assertEquals(5000.0, rating.standardFlow().to(FirePumpUnits.GPM).getValue().doubleValue(), DELTA);
	}

	@Test
	void resolveNextStandardCapacity_exceedsLargestCapacity_throwsRatherThanClamping() {
		// Largest published capacity is 5000 GPM.
		assertThrows(CalculationException.class,
				() -> resolver.resolveNextStandardCapacity(Quantities.getQuantity(5001.0, FirePumpUnits.GPM)));
	}

}
