package com.renzoproject.calc.core.mechanical.tank;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;

/**
 * Input for {@link PressureTankCalculator}. {@code drawdownFlowRate}/{@code minRunTimeMinutes}/
 * {@code lowPressureGauge}/{@code highPressureGauge} each carry a different real-world meaning
 * depending on {@link #controlType()} -- documented per field below so this isn't ambiguous to a
 * caller.
 *
 * @param controlType       conventional (on/off pressure switch) or VFD (continuously modulated)
 *                          pump control
 * @param drawdownFlowRate  <b>CONVENTIONAL</b>: the full pump flow rate -- the tank must supply
 *                          this rate for {@code minRunTimeMinutes} without the pressure dropping
 *                          below cut-in, or the pump short-cycles.
 *                          <b>VFD</b>: the pump's minimum controllable ("sleep") flow rate -- a
 *                          VFD pump throttles down rather than stopping, so drawdown is sized
 *                          against that minimum, not the full rated flow.
 * @param minRunTimeMinutes <b>CONVENTIONAL</b>: minimum meaningful pump run time to avoid
 *                          short-cycling.
 *                          <b>VFD</b>: minimum sleep duration before the pump is allowed to wake.
 * @param lowPressureGauge  <b>CONVENTIONAL</b>: cut-in pressure. <b>VFD</b>: sleep threshold
 *                          pressure.
 * @param highPressureGauge <b>CONVENTIONAL</b>: cut-out pressure. <b>VFD</b>: wake threshold
 *                          pressure; must exceed {@code lowPressureGauge}.
 * @param altitude          fed to {@code StandardAtmosphere} for gauge-to-absolute pressure
 *                          conversion; no additional range restriction beyond what
 *                          {@code StandardAtmosphere} itself enforces
 * @throws CalculationException if any of the above rules is violated
 */
public record PressureTankInput(
		PumpControlType controlType,
		Quantity<VolumetricFlowRate> drawdownFlowRate,
		double minRunTimeMinutes,
		Quantity<Pressure> lowPressureGauge,
		Quantity<Pressure> highPressureGauge,
		Quantity<Length> altitude) {

	public PressureTankInput {
		if (controlType == null) {
			throw new CalculationException("controlType is required");
		}
		if (drawdownFlowRate == null) {
			throw new CalculationException("drawdownFlowRate is required");
		}
		if (drawdownFlowRate.getValue().doubleValue() <= 0) {
			throw new CalculationException("drawdownFlowRate must be positive");
		}
		if (minRunTimeMinutes <= 0) {
			throw new CalculationException("minRunTimeMinutes must be positive");
		}
		if (lowPressureGauge == null) {
			throw new CalculationException("lowPressureGauge is required");
		}
		if (highPressureGauge == null) {
			throw new CalculationException("highPressureGauge is required");
		}
		// highPressureAbsolute > lowPressureAbsolute reduces to this raw gauge comparison, same
		// reasoning as ExpansionTankInput's fill/max-operating pressure check -- both absolute
		// pressures get the same altitude-derived atmospheric pressure added, so there's no need
		// to resolve StandardAtmosphere just to validate.
		if (highPressureGauge.to(Units.PASCAL).getValue().doubleValue()
				<= lowPressureGauge.to(Units.PASCAL).getValue().doubleValue()) {
			throw new CalculationException("highPressureGauge must be greater than lowPressureGauge "
					+ "-- the pump would never cut out (CONVENTIONAL) / the VFD would never sleep (VFD)");
		}
		if (altitude == null) {
			throw new CalculationException("altitude is required");
		}
	}

}
