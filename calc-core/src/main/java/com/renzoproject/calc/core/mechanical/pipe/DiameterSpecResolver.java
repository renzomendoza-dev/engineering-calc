package com.renzoproject.calc.core.mechanical.pipe;

import javax.measure.Quantity;
import javax.measure.quantity.Length;

/**
 * Shared {@link DiameterSpec} resolution logic — used by both {@link PipeVelocityCalculator}
 * and {@code PipePressureLossCalculator} rather than duplicated in each. Package-private since
 * only calculators within this package need it.
 */
final class DiameterSpecResolver {

	private DiameterSpecResolver() {
	}

	static Quantity<Length> resolveInternalDiameter(DiameterSpec diameterSpec, PipeDimensionResolver dimensionResolver) {
		return switch (diameterSpec) {
			case RawDiameter raw -> raw.internalDiameter();
			case NominalSize nominal ->
					dimensionResolver.resolve(nominal.material(), nominal.schedule(), nominal.nominalLabel()).internalDiameter();
		};
	}

}
