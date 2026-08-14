package com.renzoproject.calc.core.mechanical.firepump;

import com.renzoproject.calc.core.mechanical.pipe.PipeUnits;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;
import tech.units.indriya.unit.Units;

import javax.measure.Unit;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;

/**
 * This package's canonical working units — psi, GPM, feet — per the package-level Javadoc's
 * rationale. {@link #GPM} simply re-exposes {@code mechanical.pipe}'s existing
 * {@link VolumetricFlowRate} quantity type and its published GPM unit rather than defining a
 * second one, since flow rate is the same physical quantity in both packages.
 */
public final class FirePumpUnits {

	public static final Unit<VolumetricFlowRate> GPM = PipeUnits.GALLON_US_PER_MINUTE;

	/** psi = pound-force per square inch = 6894.757293168361 Pa exactly (NIST conversion factor). */
	public static final Unit<Pressure> PSI = Units.PASCAL.multiply(6894.757293168361);

	/** foot = 0.3048 m exactly (international foot). */
	public static final Unit<Length> FOOT = Units.METRE.multiply(0.3048);

	private FirePumpUnits() {
	}

}
