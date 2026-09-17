package com.renzoproject.calc.core.mechanical.duct;

import tech.units.indriya.unit.Units;

import javax.measure.Unit;

/** Derived units for duct calculations not already provided by Indriya's {@link Units}. */
public final class DuctUnits {

	public static final Unit<PressureGradient> PASCAL_PER_METRE =
			Units.PASCAL.divide(Units.METRE).asType(PressureGradient.class);

	private DuctUnits() {
	}

}
