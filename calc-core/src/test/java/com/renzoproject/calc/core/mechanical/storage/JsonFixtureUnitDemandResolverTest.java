package com.renzoproject.calc.core.mechanical.storage;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.PipeUnits;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link JsonFixtureUnitDemandResolver} against the real {@code wsfu-demand.json}
 * reference data.
 */
class JsonFixtureUnitDemandResolverTest {

	private static final double DELTA = 1e-9;

	private final JsonFixtureUnitDemandResolver resolver = new JsonFixtureUnitDemandResolver();

	@Test
	void resolveGpm_exactRowMatch_returnsPublishedValueDirectly() {
		assertEquals(44.0, resolver.resolveGpm(100.0, SystemType.FLUSH_TANK), DELTA);
		assertEquals(68.0, resolver.resolveGpm(100.0, SystemType.FLUSH_VALVE), DELTA);
	}

	@Test
	void resolveGpm_flushTank_interpolatesBetweenBracketingRows() {
		// wsfu=7 -> 6 GPM, wsfu=8 -> 7 GPM; 7.5 is the midpoint -> 6.5 GPM.
		assertEquals(6.5, resolver.resolveGpm(7.5, SystemType.FLUSH_TANK), DELTA);
	}

	@Test
	void resolveGpm_flushValve_interpolatesBetweenBracketingRows() {
		// wsfu=7 -> 24 GPM, wsfu=8 -> 25 GPM; 7.5 is the midpoint -> 24.5 GPM.
		assertEquals(24.5, resolver.resolveGpm(7.5, SystemType.FLUSH_VALVE), DELTA);
	}

	@Test
	void resolveGpm_1866Wsfu_matchesReferenceReadmeCrossCheckWithinTolerance() {
		// reference/storage/README.md: the Revised National Plumbing Code of the Philippines'
		// own worked example is 1,866 WSFU -> 19.7 L/s; interpolating this table gives 19.66 L/s
		// (a 0.21% deviation) -- regression check against that documented cross-validation.
		double gpm = resolver.resolveGpm(1866.0, SystemType.FLUSH_TANK);
		double litresPerSecond = Quantities.getQuantity(gpm, PipeUnits.GALLON_US_PER_MINUTE)
				.to(PipeUnits.LITRE_PER_SECOND).getValue().doubleValue();

		assertEquals(19.66, litresPerSecond, 0.01);
	}

	@Test
	void resolveGpm_bothSystemTypes_convergeAboveWsfu1000() {
		double flushTank = resolver.resolveGpm(1866.0, SystemType.FLUSH_TANK);
		double flushValve = resolver.resolveGpm(1866.0, SystemType.FLUSH_VALVE);

		assertEquals(flushTank, flushValve, DELTA);
	}

	@Test
	void resolveGpm_exactlyAtUpperBound_returnsPublishedValue() {
		assertEquals(790.0, resolver.resolveGpm(10000.0, SystemType.FLUSH_TANK), DELTA);
	}

	@Test
	void resolveGpm_exceedsUpperBound_throws() {
		assertThrows(CalculationException.class, () -> resolver.resolveGpm(10001.0, SystemType.FLUSH_TANK));
	}

	@Test
	void resolveGpm_exactlyAtFlushValveMinimum_returnsPublishedValue() {
		assertEquals(22.0, resolver.resolveGpm(5.0, SystemType.FLUSH_VALVE), DELTA);
	}

	@Test
	void resolveGpm_flushValveBelowMinimum_throws() {
		assertThrows(CalculationException.class, () -> resolver.resolveGpm(4.0, SystemType.FLUSH_VALVE));
	}

	@Test
	void resolveGpm_flushTankBelowTableMinimum_throws() {
		// Table's smallest row is wsfu=3 -- below that, there's no lower bracket to interpolate
		// from, so this must throw rather than extrapolate (same principle as the upper bound).
		assertThrows(CalculationException.class, () -> resolver.resolveGpm(1.0, SystemType.FLUSH_TANK));
	}

	@Test
	void resolveGpm_nullSystemType_throws() {
		assertThrows(CalculationException.class, () -> resolver.resolveGpm(100.0, null));
	}

}
