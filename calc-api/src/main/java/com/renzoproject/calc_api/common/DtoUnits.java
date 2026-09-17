package com.renzoproject.calc_api.common;

import tech.units.indriya.unit.Units;

import javax.measure.MetricPrefix;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;

/**
 * Prefixed SI units that request/response DTO fields are expressed in, shared by every mapper that
 * converts at the API boundary (e.g. a {@code ...Mm} or {@code ...Kpa} field).
 *
 * <p>These live in calc-api rather than calc-core because calc-core works in base SI units
 * internally; only the HTTP boundary uses these industry-conventional scales. Domain-derived units
 * such as flow rate stay in calc-core's own unit classes ({@code PipeUnits}, {@code FirePumpUnits},
 * {@code StorageUnits}).
 */
public final class DtoUnits {

	public static final Unit<Length> MILLIMETRE = MetricPrefix.MILLI(Units.METRE);

	public static final Unit<Pressure> KILOPASCAL = MetricPrefix.KILO(Units.PASCAL);

	private DtoUnits() {
	}

}
