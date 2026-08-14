package com.renzoproject.calc.core.mechanical.pipe;

import tech.units.indriya.unit.Units;

import javax.measure.Unit;

/**
 * Derived units for pipe flow calculations not already provided by Indriya's {@link Units}.
 * {@code LITRE_PER_SECOND}/{@code LITRE_PER_MINUTE} are included alongside the SI base unit
 * since plumbing flow rates are conventionally specified in LPM/LPS, not m3/s — the whole point
 * of using Indriya quantities is that callers can supply whichever unit is natural and the
 * calculator converts internally.
 */
public final class PipeUnits {

	public static final Unit<VolumetricFlowRate> CUBIC_METRE_PER_SECOND =
			Units.CUBIC_METRE.divide(Units.SECOND).asType(VolumetricFlowRate.class);

	public static final Unit<VolumetricFlowRate> LITRE_PER_SECOND =
			Units.LITRE.divide(Units.SECOND).asType(VolumetricFlowRate.class);

	public static final Unit<VolumetricFlowRate> LITRE_PER_MINUTE =
			Units.LITRE.divide(Units.MINUTE).asType(VolumetricFlowRate.class);

	/** US liquid gallon = 3.785411784 L exactly (231 in³, by definition). */
	public static final Unit<VolumetricFlowRate> GALLON_US_PER_MINUTE =
			Units.LITRE.multiply(3.785411784).divide(Units.MINUTE).asType(VolumetricFlowRate.class);

	private PipeUnits() {
	}

}
