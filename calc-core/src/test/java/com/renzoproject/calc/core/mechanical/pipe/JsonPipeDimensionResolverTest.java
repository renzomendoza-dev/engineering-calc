package com.renzoproject.calc.core.mechanical.pipe;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.MetricPrefix;
import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link JsonPipeDimensionResolver} against the real {@code gi.json} reference data
 * (verified/stable, per {@code reference/pipes/README.md}), to confirm the loader itself works
 * correctly — separate from {@link PipeVelocityCalculatorTest}, which uses a fake resolver so
 * it isn't coupled to these exact published numbers.
 */
class JsonPipeDimensionResolverTest {

	private static final double DELTA = 1e-9;
	private static final Unit<Length> MILLIMETRE = MetricPrefix.MILLI(Units.METRE);

	private final JsonPipeDimensionResolver resolver = new JsonPipeDimensionResolver();

	private static Quantity<Length> lengthMm(double value) {
		return Quantities.getQuantity(value, MILLIMETRE);
	}

	@Test
	void resolve_giSch40Two_matchesPublishedDimensions() {
		PipeDimension dimension = resolver.resolve("GI", "SCH40", "2");

		assertEquals("2", dimension.nominalSize());
		assertEquals("2\" (DN50)", dimension.nominalLabel());
		assertEquals(52.48, dimension.internalDiameter().to(MILLIMETRE).getValue().doubleValue(), DELTA);
		assertEquals(60.3, dimension.outsideDiameter().to(MILLIMETRE).getValue().doubleValue(), DELTA);
	}

	@Test
	void resolve_isCaseInsensitiveOnMaterialAndSchedule() {
		PipeDimension dimension = resolver.resolve("gi", "sch40", "2");

		assertEquals(52.48, dimension.internalDiameter().to(MILLIMETRE).getValue().doubleValue(), DELTA);
	}

	@Test
	void resolve_unknownNominalSize_throws() {
		assertThrows(CalculationException.class, () -> resolver.resolve("GI", "SCH40", "99"));
	}

	@Test
	void resolve_unknownMaterial_throws() {
		assertThrows(CalculationException.class, () -> resolver.resolve("TITANIUM", "SCH40", "2"));
	}

	@Test
	void resolve_unknownSchedule_throws() {
		// GI only publishes SCH40.
		assertThrows(CalculationException.class, () -> resolver.resolve("GI", "SCH80", "2"));
	}

	@Test
	void resolveNextStandardSize_roundsUpToSmallestSufficientSize() {
		// Published GI SCH40 sizes bracket 50mm at "1-1/2" (40.94mm, too small) and "2" (52.48mm).
		PipeDimension dimension = resolver.resolveNextStandardSize(lengthMm(50.0), "GI", "SCH40");

		assertEquals("2", dimension.nominalSize());
		assertEquals(52.48, dimension.internalDiameter().to(MILLIMETRE).getValue().doubleValue(), DELTA);
	}

	@Test
	void resolveNextStandardSize_exactMatchIsSufficient() {
		// Requesting exactly the published internal diameter of "2" (52.48mm) must resolve to
		// "2" itself, not round up further -- ">=" not ">".
		PipeDimension dimension = resolver.resolveNextStandardSize(lengthMm(52.48), "GI", "SCH40");

		assertEquals("2", dimension.nominalSize());
	}

	@Test
	void resolveNextStandardSize_noPublishedSizeLargeEnough_throwsRatherThanClamping() {
		// Largest published GI SCH40 size is "6" at 154.08mm.
		assertThrows(CalculationException.class,
				() -> resolver.resolveNextStandardSize(lengthMm(1000.0), "GI", "SCH40"));
	}

	@Test
	void listAllMaterials_returnsAllFourMaterialsInFixedOrderWithConfidenceFlags() {
		List<PipeMaterialReference> materials = resolver.listAllMaterials();

		assertEquals(4, materials.size());
		assertEquals(List.of("GI", "BI", "UPVC", "PPR"), materials.stream().map(PipeMaterialReference::material).toList());
		assertEquals("verified", materials.get(0).confidence());
		assertEquals("placeholder", materials.get(2).confidence()); // UPVC
		assertEquals("placeholder", materials.get(3).confidence()); // PPR
	}

	@Test
	void listAllMaterials_giIncludesSch40WithPublishedTwoInchDimensions() {
		PipeMaterialReference gi = resolver.listAllMaterials().get(0);

		assertEquals("GI", gi.material());
		assertEquals(1, gi.schedules().size());
		assertEquals("SCH40", gi.schedules().get(0).schedule());
		PipeSizeReference two = gi.schedules().get(0).sizes().stream()
				.filter(size -> size.nominalSize().equals("2"))
				.findFirst()
				.orElseThrow();
		assertEquals("2\" (DN50)", two.nominalLabel());
		assertEquals(52.48, two.internalDiameterMm(), DELTA);
	}

	@Test
	void listAllMaterials_pprIncludesAllThreeSchedules() {
		PipeMaterialReference ppr = resolver.listAllMaterials().get(3);

		assertTrue(ppr.schedules().stream().map(PipeScheduleReference::schedule)
				.toList().containsAll(List.of("PN10", "PN16", "PN20")));
	}

}
