package com.renzoproject.calc.core.mechanical.storage;

import tech.units.indriya.unit.Units;

import javax.measure.Unit;
import javax.measure.quantity.Volume;

/**
 * Derived units for water storage calculations not already provided by Indriya's {@link Units}.
 * {@code Volume} (unlike {@code VolumetricFlowRate}) is a standard JSR-385 quantity, so
 * {@code Units.LITRE}/{@code Units.CUBIC_METRE} are already {@code Unit<Volume>} directly --
 * only the US gallon needs defining here.
 */
public final class StorageUnits {

	/** US liquid gallon = 3.785411784 L exactly (231 in3, by definition) -- same conversion
	 * factor as {@code PipeUnits.GALLON_US_PER_MINUTE}. */
	public static final Unit<Volume> GALLON_US = Units.LITRE.multiply(3.785411784);

	private StorageUnits() {
	}

}
