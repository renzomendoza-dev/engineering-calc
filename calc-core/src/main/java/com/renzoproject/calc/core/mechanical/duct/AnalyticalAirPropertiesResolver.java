package com.renzoproject.calc.core.mechanical.duct;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.FluidProperties;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Temperature;

/**
 * Analytical air properties (density, viscosity) as a function of temperature and altitude -- no
 * JSON reference file, pure formula:
 * <ul>
 *   <li>Barometric pressure by altitude: International Standard Atmosphere,
 *       {@code P(h) = P0 * (1 - L*h/T0)^(g*M/(R*L))}.</li>
 *   <li>Density via the ideal gas law: {@code rho = P / (R_specific * T)}, dry air.</li>
 *   <li>Viscosity via Sutherland's Law: {@code mu = mu_ref * (T_ref+S)/(T+S) * (T/T_ref)^1.5}.</li>
 * </ul>
 *
 * <p><b>Dry air only -- no humidity correction.</b> Cross-validated against ASHRAE Fundamentals
 * Chapter 3's stated standard air properties (20 degC, 101.325 kPa: density 1.21 kg/m3, viscosity
 * 18.1 uN*s/m2): this resolver reproduces viscosity almost exactly (18.13 vs. 18.1 uN*s/m2) and
 * density within normal rounding (1.204 vs. 1.21 kg/m3) -- see
 * {@link AnalyticalAirPropertiesResolverTest} and {@code reference/duct/README.md}.
 */
public class AnalyticalAirPropertiesResolver implements AirPropertiesResolver {

	private static final double SEA_LEVEL_PRESSURE_PA = 101325.0;
	private static final double TEMPERATURE_LAPSE_RATE_K_PER_M = 0.0065;
	private static final double SEA_LEVEL_STANDARD_TEMPERATURE_K = 288.15;
	private static final double GRAVITY_M_S2 = 9.80665;
	private static final double MOLAR_MASS_AIR_KG_PER_MOL = 0.0289644;
	private static final double UNIVERSAL_GAS_CONSTANT_J_PER_MOL_K = 8.31447;
	private static final double BAROMETRIC_EXPONENT =
			(GRAVITY_M_S2 * MOLAR_MASS_AIR_KG_PER_MOL) / (UNIVERSAL_GAS_CONSTANT_J_PER_MOL_K * TEMPERATURE_LAPSE_RATE_K_PER_M);

	private static final double SPECIFIC_GAS_CONSTANT_DRY_AIR_J_PER_KG_K = 287.05;

	private static final double SUTHERLAND_REFERENCE_VISCOSITY_PA_S = 1.716e-5;
	private static final double SUTHERLAND_REFERENCE_TEMPERATURE_K = 273.15;
	private static final double SUTHERLAND_CONSTANT_K = 110.4;

	private static final double CELSIUS_TO_KELVIN_OFFSET = 273.15;

	@Override
	public FluidProperties resolve(Quantity<Temperature> temperature, Quantity<Length> altitude) {
		if (temperature == null) {
			throw new CalculationException("temperature is required");
		}
		if (altitude == null) {
			throw new CalculationException("altitude is required");
		}

		double temperatureK = temperature.to(Units.CELSIUS).getValue().doubleValue() + CELSIUS_TO_KELVIN_OFFSET;
		if (temperatureK <= 0) {
			throw new CalculationException("temperature " + (temperatureK - CELSIUS_TO_KELVIN_OFFSET)
					+ " degC is not physically valid (at or below absolute zero)");
		}

		double altitudeM = altitude.to(Units.METRE).getValue().doubleValue();
		double pressurePa = barometricPressure(altitudeM);

		double densityKgM3 = pressurePa / (SPECIFIC_GAS_CONSTANT_DRY_AIR_J_PER_KG_K * temperatureK);
		double viscosityPaS = sutherlandViscosity(temperatureK);

		return new FluidProperties(densityKgM3, viscosityPaS);
	}

	private static double barometricPressure(double altitudeM) {
		double base = 1.0 - (TEMPERATURE_LAPSE_RATE_K_PER_M * altitudeM) / SEA_LEVEL_STANDARD_TEMPERATURE_K;
		if (base <= 0) {
			throw new CalculationException("altitude " + altitudeM + " m is outside the International Standard "
					+ "Atmosphere formula's valid range (the base term went non-positive)");
		}
		return SEA_LEVEL_PRESSURE_PA * Math.pow(base, BAROMETRIC_EXPONENT);
	}

	private static double sutherlandViscosity(double temperatureK) {
		double referenceRatio = (SUTHERLAND_REFERENCE_TEMPERATURE_K + SUTHERLAND_CONSTANT_K) / (temperatureK + SUTHERLAND_CONSTANT_K);
		double temperatureRatio = Math.pow(temperatureK / SUTHERLAND_REFERENCE_TEMPERATURE_K, 1.5);
		return SUTHERLAND_REFERENCE_VISCOSITY_PA_S * referenceRatio * temperatureRatio;
	}

}
