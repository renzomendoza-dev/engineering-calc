package com.renzoproject.calc.core.mechanical.standpipe;

import com.renzoproject.calc.core.mechanical.firepump.FirePumpUnits;
import javax.measure.Unit;
import javax.measure.quantity.Length;

/**
 * The one unit this package needs that {@link FirePumpUnits} does not publish: the inch, for the
 * inside diameter in the Hazen-Williams formula.
 */
public final class StandpipeUnits {

	/** inch = 1/12 ft exactly. */
	public static final Unit<Length> INCH = FirePumpUnits.FOOT.divide(12);

	private StandpipeUnits() {
	}

}
