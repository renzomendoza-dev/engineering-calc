package com.renzoproject.calc_api.mechanical.pipe;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.DiameterSpec;
import com.renzoproject.calc.core.mechanical.pipe.NominalSize;
import com.renzoproject.calc.core.mechanical.pipe.PipeUnits;
import com.renzoproject.calc.core.mechanical.pipe.RawDiameter;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.MetricPrefix;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import java.util.Map;

/**
 * Shared unit-string parsing and {@link DiameterSpec} construction, used by both
 * {@link PipeVelocityMapper} and {@link PipePressureLossMapper} so the two mappers don't each
 * duplicate the NOMINAL/RAW branching or the flow-rate/length unit maps — {@code flowRateUnit}
 * handling in particular needs to behave identically across both endpoints, not just similarly.
 * Package-private since only mappers in this package need it.
 */
final class PipeUnitParsing {

	private static final Unit<Length> MILLIMETRE = MetricPrefix.MILLI(Units.METRE);

	private static final Map<String, Unit<VolumetricFlowRate>> FLOW_RATE_UNITS = Map.of(
			"m3/s", PipeUnits.CUBIC_METRE_PER_SECOND,
			"m3/hr", Units.CUBIC_METRE.divide(Units.HOUR).asType(VolumetricFlowRate.class),
			"L/s", PipeUnits.LITRE_PER_SECOND,
			"L/min", PipeUnits.LITRE_PER_MINUTE,
			"gpm", PipeUnits.GALLON_US_PER_MINUTE);

	private static final Map<String, Unit<Length>> LENGTH_UNITS = Map.of(
			"mm", MILLIMETRE,
			"cm", MetricPrefix.CENTI(Units.METRE),
			"m", Units.METRE,
			"in", Units.METRE.multiply(0.0254));

	private PipeUnitParsing() {
	}

	static Unit<VolumetricFlowRate> parseFlowRateUnit(String rawValue) {
		Unit<VolumetricFlowRate> unit = FLOW_RATE_UNITS.get(rawValue);
		if (unit == null) {
			throw new CalculationException("Unknown flow rate unit: " + rawValue + " (supported: " + FLOW_RATE_UNITS.keySet() + ")");
		}
		return unit;
	}

	static Unit<Length> parseLengthUnit(String rawValue) {
		Unit<Length> unit = LENGTH_UNITS.get(rawValue);
		if (unit == null) {
			throw new CalculationException("Unknown length unit: " + rawValue + " (supported: " + LENGTH_UNITS.keySet() + ")");
		}
		return unit;
	}

	static DiameterSpec toCoreDiameterSpec(
			DiameterSpecTypeDto diameterSpecType,
			String nominalMaterial,
			String nominalSchedule,
			String nominalLabel,
			Double rawDiameterValue,
			String rawDiameterUnit) {
		return switch (diameterSpecType) {
			case NOMINAL -> new NominalSize(nominalMaterial, nominalSchedule, nominalLabel);
			case RAW -> new RawDiameter(Quantities.getQuantity(rawDiameterValue, parseLengthUnit(rawDiameterUnit)));
		};
	}

}
