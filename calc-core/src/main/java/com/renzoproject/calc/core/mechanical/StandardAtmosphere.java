package com.renzoproject.calc.core.mechanical;

import com.renzoproject.calc.core.exception.CalculationException;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;

/**
 * International Standard Atmosphere barometric pressure formula, {@code P(h) = P0 * (1 -
 * L*h/T0)^(g*M/(R*L))}. Extracted from {@code mechanical.duct.AnalyticalAirDensityViscosityResolver}
 * (which used it only to derive air density) so {@code mechanical.tank.ExpansionTankCalculator}
 * can reuse it too, for gauge-to-absolute pressure conversion at altitude -- pure extraction, no
 * behavior change.
 */
public final class StandardAtmosphere {

	private static final double SEA_LEVEL_PRESSURE_PA = 101325.0;
	private static final double TEMPERATURE_LAPSE_RATE_K_PER_M = 0.0065;
	private static final double SEA_LEVEL_STANDARD_TEMPERATURE_K = 288.15;
	private static final double GRAVITY_M_S2 = 9.80665;
	private static final double MOLAR_MASS_AIR_KG_PER_MOL = 0.0289644;
	private static final double UNIVERSAL_GAS_CONSTANT_J_PER_MOL_K = 8.31447;
	private static final double BAROMETRIC_EXPONENT =
			(GRAVITY_M_S2 * MOLAR_MASS_AIR_KG_PER_MOL) / (UNIVERSAL_GAS_CONSTANT_J_PER_MOL_K * TEMPERATURE_LAPSE_RATE_K_PER_M);

	private StandardAtmosphere() {
	}

	/**
	 * @throws CalculationException if {@code altitude} is null, or is outside the formula's valid
	 *                              range (the base term goes non-positive)
	 */
	public static Quantity<Pressure> pressureAtAltitude(Quantity<Length> altitude) {
		if (altitude == null) {
			throw new CalculationException("altitude is required");
		}

		double altitudeM = altitude.to(Units.METRE).getValue().doubleValue();
		double base = 1.0 - (TEMPERATURE_LAPSE_RATE_K_PER_M * altitudeM) / SEA_LEVEL_STANDARD_TEMPERATURE_K;
		if (base <= 0) {
			throw new CalculationException("altitude " + altitudeM + " m is outside the International Standard "
					+ "Atmosphere formula's valid range (the base term went non-positive)");
		}

		double pressurePa = SEA_LEVEL_PRESSURE_PA * Math.pow(base, BAROMETRIC_EXPONENT);
		return Quantities.getQuantity(pressurePa, Units.PASCAL);
	}

}
