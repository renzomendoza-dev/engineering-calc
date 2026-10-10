package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.mechanical.firepump.FirePumpUnits;
import tech.units.indriya.unit.Units;

import javax.measure.Unit;
import javax.measure.quantity.Length;
import javax.measure.quantity.Speed;

/**
 * Units this package needs that aren't published elsewhere in calc-core. psi, GPM and feet are
 * reused directly from {@link FirePumpUnits} rather than redefined -- they are the same units, and
 * that class is already the codebase's home for the US fire-protection working units (see its
 * package-level Javadoc for why those units are used at all).
 */
public final class SprinklerUnits {

	/** inch = 1/12 ft exactly. */
	public static final Unit<Length> INCH = FirePumpUnits.FOOT.divide(12);

	/** feet per second, the conventional unit for sprinkler pipe velocity limits. */
	public static final Unit<Speed> FOOT_PER_SECOND = FirePumpUnits.FOOT.divide(Units.SECOND).asType(Speed.class);

	private SprinklerUnits() {
	}

}
