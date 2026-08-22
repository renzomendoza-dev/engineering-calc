package com.renzoproject.calc.core.mechanical;

import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Volume;

/**
 * Boyle's Law bladder/diaphragm tank-sizing formula, shared by {@code mechanical.tank}'s
 * {@code ExpansionTankCalculator} and {@code PressureTankCalculator}: both size a gas-charged
 * tank to absorb a usable liquid volume swing between two absolute pressures --
 * {@code requiredVolume = usableVolume / (1 - Plow/Phigh)} -- despite representing physically
 * distinct problems (thermal expansion vs. pump-cycling drawdown). One occurrence of a formula
 * this small wouldn't be worth extracting, but a second, unrelated calculator needing the exact
 * same shape is -- if a future tank calculator needs it again, reuse this rather than writing a
 * third copy.
 */
public final class BoyleTankSizing {

	private BoyleTankSizing() {
	}

	/**
	 * @param usableVolume        the liquid volume the tank must absorb (expansion volume,
	 *                             drawdown volume, ...)
	 * @param lowPressureAbsolute the tank's low-end absolute pressure (fill / cut-in / sleep)
	 * @param highPressureAbsolute the tank's high-end absolute pressure (max operating / cut-out /
	 *                             wake); callers are responsible for ensuring this exceeds
	 *                             {@code lowPressureAbsolute} -- this method doesn't itself
	 *                             validate, since both current callers already enforce that rule
	 *                             on their own gauge-pressure inputs before reaching this formula
	 */
	public static Quantity<Volume> requiredVolume(
			Quantity<Volume> usableVolume, Quantity<Pressure> lowPressureAbsolute, Quantity<Pressure> highPressureAbsolute) {
		double usableVolumeM3 = usableVolume.to(Units.CUBIC_METRE).getValue().doubleValue();
		double lowPa = lowPressureAbsolute.to(Units.PASCAL).getValue().doubleValue();
		double highPa = highPressureAbsolute.to(Units.PASCAL).getValue().doubleValue();
		double requiredVolumeM3 = usableVolumeM3 / (1.0 - lowPa / highPa);
		return Quantities.getQuantity(requiredVolumeM3, Units.CUBIC_METRE);
	}

}
