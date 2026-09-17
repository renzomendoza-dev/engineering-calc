package com.renzoproject.calc.core.mechanical.pipe;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Temperature;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link JsonFluidPropertiesResolver} against the real {@code water.json} reference data
 * (verified, per {@code reference/fluids/fluids-README.md}), to confirm the loader itself works
 * correctly — separate from {@link PipePressureLossCalculatorTest}, which uses a fake resolver
 * so it isn't coupled to these exact published numbers.
 */
class JsonFluidPropertiesResolverTest {

	private static final double DELTA = 1e-9;

	private final JsonFluidPropertiesResolver resolver = new JsonFluidPropertiesResolver();

	private static Quantity<Temperature> celsius(double value) {
		return Quantities.getQuantity(value, Units.CELSIUS);
	}

	@Test
	void resolve_exactTableRow_returnsPublishedValues() {
		// Published: 20 degC -> density=998.2, dynamicViscosity=0.001002.
		FluidProperties properties = resolver.resolve("WATER", celsius(20.0));

		assertEquals(998.2, properties.densityKgM3(), DELTA);
		assertEquals(0.001002, properties.dynamicViscosityPaS(), DELTA);
	}

	@Test
	void resolve_isCaseInsensitiveOnFluidKey() {
		FluidProperties properties = resolver.resolve("water", celsius(20.0));

		assertEquals(998.2, properties.densityKgM3(), DELTA);
	}

	@Test
	void resolve_interpolatesBetweenBracketingRows() {
		// Published: 20 degC -> 998.2/0.001002, 30 degC -> 995.6/0.000798. At 25 degC (midpoint):
		// density=996.9000000000001, dynamicViscosity=0.0009 (precomputed).
		FluidProperties properties = resolver.resolve("WATER", celsius(25.0));

		assertEquals(996.9000000000001, properties.densityKgM3(), DELTA);
		assertEquals(0.0009, properties.dynamicViscosityPaS(), DELTA);
	}

	@Test
	void resolve_temperatureBelowTableRange_throwsRatherThanExtrapolating() {
		// Published table starts at 0 degC.
		assertThrows(CalculationException.class, () -> resolver.resolve("WATER", celsius(-5.0)));
	}

	@Test
	void resolve_temperatureAboveTableRange_throwsRatherThanExtrapolating() {
		// Published table ends at 100 degC.
		assertThrows(CalculationException.class, () -> resolver.resolve("WATER", celsius(105.0)));
	}

	@Test
	void resolve_unknownFluidKey_throws() {
		assertThrows(CalculationException.class, () -> resolver.resolve("GLYCOL", celsius(20.0)));
	}

	@Test
	void resolve_concurrentFirstCallsOnSharedInstance_allReturnCorrectValues() throws Exception {
		// calc-api shares one instance across request threads, and the cache fills lazily on first
		// use -- so the risky moment is many threads racing to populate it at once. All threads
		// release together on the latch to maximise overlap. This can't deterministically reproduce
		// a HashMap race, but it exercises the exact access pattern that motivated the
		// ConcurrentHashMap and would surface corruption or exceptions if one regressed.
		JsonFluidPropertiesResolver shared = new JsonFluidPropertiesResolver();
		int threads = 32;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		try {
			List<Future<FluidProperties>> futures = new ArrayList<>();
			for (int i = 0; i < threads; i++) {
				String key = i % 2 == 0 ? "WATER" : "water";
				futures.add(pool.submit(() -> {
					start.await();
					return shared.resolve(key, celsius(20.0));
				}));
			}
			start.countDown();

			for (Future<FluidProperties> future : futures) {
				FluidProperties properties = future.get(10, TimeUnit.SECONDS);
				assertEquals(998.2, properties.densityKgM3(), DELTA);
				assertEquals(0.001002, properties.dynamicViscosityPaS(), DELTA);
			}
		} finally {
			pool.shutdownNow();
		}
	}

}
