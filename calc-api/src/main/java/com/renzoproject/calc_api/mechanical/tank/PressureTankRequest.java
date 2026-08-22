package com.renzoproject.calc_api.mechanical.tank;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * HTTP request body for a hydropneumatic pressure tank sizing calculation. SI throughout, same
 * boundary-unit convention as {@code ExpansionTankRequest} (liters for volume, kPa for pressure,
 * meters for altitude); {@code drawdownFlowRateLpm} uses L/min rather than L/s -- minutes is the
 * more natural time unit for pump run-time/cycling context.
 *
 * <p>{@code drawdownFlowRateLpm}, {@code minRunTimeMinutes}, {@code lowPressureGaugeKpa}, and
 * {@code highPressureGaugeKpa} each carry a different real-world meaning depending on
 * {@code controlType} -- mirrors calc-core's {@code PressureTankInput} Javadoc exactly so the API
 * documentation and the core documentation don't drift apart:
 *
 * @param controlType         conventional (on/off pressure switch) or VFD (continuously
 *                            modulated) pump control
 * @param drawdownFlowRateLpm <b>CONVENTIONAL</b>: the full pump flow rate -- the tank must supply
 *                            this rate for {@code minRunTimeMinutes} without the pressure dropping
 *                            below cut-in, or the pump short-cycles.
 *                            <b>VFD</b>: the pump's minimum controllable ("sleep") flow rate -- a
 *                            VFD pump throttles down rather than stopping, so drawdown is sized
 *                            against that minimum, not the full rated flow.
 * @param minRunTimeMinutes   <b>CONVENTIONAL</b>: minimum meaningful pump run time to avoid
 *                            short-cycling. <b>VFD</b>: minimum sleep duration before the pump is
 *                            allowed to wake.
 * @param lowPressureGaugeKpa  <b>CONVENTIONAL</b>: cut-in pressure. <b>VFD</b>: sleep threshold
 *                            pressure.
 * @param highPressureGaugeKpa <b>CONVENTIONAL</b>: cut-out pressure. <b>VFD</b>: wake threshold
 *                            pressure; must exceed {@code lowPressureGaugeKpa}.
 * @param altitudeMeters      fed to {@code StandardAtmosphere} for gauge-to-absolute pressure
 *                            conversion
 */
public record PressureTankRequest(
		@NotNull PumpControlTypeDto controlType,
		@NotNull @Positive Double drawdownFlowRateLpm,
		@NotNull @Positive Double minRunTimeMinutes,
		@NotNull @Positive Double lowPressureGaugeKpa,
		@NotNull @Positive Double highPressureGaugeKpa,
		@NotNull Double altitudeMeters) {

}
