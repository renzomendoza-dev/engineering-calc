package com.renzoproject.calc.core.mechanical.pipe;

import com.renzoproject.calc.core.exception.CalculationException;
import tech.units.indriya.unit.Units;

import javax.measure.MetricPrefix;
import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import java.util.List;

/**
 * Minimal in-memory {@link PipeDimensionResolver} for {@link PipeVelocityCalculatorTest}, so
 * calculator tests aren't coupled to the real reference data's exact numbers.
 */
class FakePipeDimensionResolver implements PipeDimensionResolver {

	private static final Unit<Length> MILLIMETRE = MetricPrefix.MILLI(Units.METRE);

	record FakeSize(String material, String schedule, PipeDimension dimension) {

		static FakeSize of(String material, String schedule, String nominalSize, String nominalLabel, double internalDiameterMm) {
			PipeDimension dimension = new PipeDimension(
					nominalSize,
					nominalLabel,
					tech.units.indriya.quantity.Quantities.getQuantity(internalDiameterMm, MILLIMETRE),
					tech.units.indriya.quantity.Quantities.getQuantity(internalDiameterMm + 5, MILLIMETRE));
			return new FakeSize(material, schedule, dimension);
		}
	}

	private final List<FakeSize> sizes;

	FakePipeDimensionResolver(List<FakeSize> sizes) {
		this.sizes = sizes;
	}

	@Override
	public PipeDimension resolve(String material, String schedule, String nominalLabel) {
		return sizes.stream()
				.filter(s -> s.material().equalsIgnoreCase(material) && s.schedule().equalsIgnoreCase(schedule)
						&& s.dimension().nominalSize().equalsIgnoreCase(nominalLabel))
				.map(FakeSize::dimension)
				.findFirst()
				.orElseThrow(() -> new CalculationException(
						"Fake resolver: no size for " + material + "/" + schedule + "/" + nominalLabel));
	}

	@Override
	public PipeDimension resolveNextStandardSize(Quantity<Length> calculatedMinDiameter, String material, String schedule) {
		double minMm = calculatedMinDiameter.to(MILLIMETRE).getValue().doubleValue();
		return sizes.stream()
				.filter(s -> s.material().equalsIgnoreCase(material) && s.schedule().equalsIgnoreCase(schedule))
				.map(FakeSize::dimension)
				.filter(d -> d.internalDiameter().to(MILLIMETRE).getValue().doubleValue() >= minMm)
				.min((a, b) -> Double.compare(
						a.internalDiameter().to(MILLIMETRE).getValue().doubleValue(),
						b.internalDiameter().to(MILLIMETRE).getValue().doubleValue()))
				.orElseThrow(() -> new CalculationException(
						"Fake resolver: no standard size >= " + minMm + "mm for " + material + "/" + schedule));
	}

	@Override
	public List<PipeMaterialReference> listAllMaterials() {
		throw new UnsupportedOperationException("not exercised by PipeVelocityCalculatorTest");
	}

}
