package com.renzoproject.calc.core.mechanical.tank;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.mechanical.BoyleTankSizing;
import com.renzoproject.calc.core.mechanical.StandardAtmosphere;
import com.renzoproject.calc.core.mechanical.pipe.PipeUnits;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Volume;

/**
 * Hydropneumatic (bladder/diaphragm) pressure tank sizing for well pump / booster pump systems,
 * for both conventional (on/off pressure switch) and VFD (continuously modulated) pump control.
 *
 * <p>A separate calculator from {@link ExpansionTankCalculator} -- same Boyle's Law tank-sizing
 * shape (see {@link BoyleTankSizing}) and the same {@code StandardAtmosphere} dependency, but a
 * physically distinct sizing problem (pump-cycling drawdown, not thermal expansion), so the two
 * stay independently understandable rather than merged into one input type.
 */
public class PressureTankCalculator implements Calculator<PressureTankInput, PressureTankResult> {

	@Override
	public PressureTankResult calculate(PressureTankInput input) {
		double drawdownFlowRateLpm = input.drawdownFlowRate().to(PipeUnits.LITRE_PER_MINUTE).getValue().doubleValue();
		double drawdownVolumeLiters = drawdownFlowRateLpm * input.minRunTimeMinutes();
		Quantity<Volume> drawdownVolume = Quantities.getQuantity(drawdownVolumeLiters, Units.LITRE);

		Quantity<Pressure> atmosphericPressure = StandardAtmosphere.pressureAtAltitude(input.altitude());
		Quantity<Pressure> lowPressureAbsolute = input.lowPressureGauge().add(atmosphericPressure);
		Quantity<Pressure> highPressureAbsolute = input.highPressureGauge().add(atmosphericPressure);

		Quantity<Volume> requiredTankVolume =
				BoyleTankSizing.requiredVolume(drawdownVolume, lowPressureAbsolute, highPressureAbsolute);

		return new PressureTankResult(drawdownVolume, requiredTankVolume, lowPressureAbsolute, highPressureAbsolute);
	}

}
