package com.renzoproject.calc_api.mechanical.tank;

import com.renzoproject.calc.core.mechanical.pipe.PipeUnits;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;
import com.renzoproject.calc.core.mechanical.tank.PressureTankInput;
import com.renzoproject.calc.core.mechanical.tank.PressureTankResult;
import com.renzoproject.calc.core.mechanical.tank.PumpControlType;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;

import static com.renzoproject.calc_api.common.DtoUnits.KILOPASCAL;

/**
 * Pure mapping, no logic -- same conversion approach as {@link ExpansionTankMapper} (liters&lt;-&gt;m3,
 * kPa&lt;-&gt;Pa), since both endpoints share the same Boyle's Law output shape.
 */
public final class PressureTankMapper {

	private PressureTankMapper() {
	}

	public static PressureTankInput toCoreInput(PressureTankRequest request) {
		Quantity<VolumetricFlowRate> drawdownFlowRate = Quantities.getQuantity(request.drawdownFlowRateLpm(), PipeUnits.LITRE_PER_MINUTE);
		Quantity<Pressure> lowPressureGauge = Quantities.getQuantity(request.lowPressureGaugeKpa(), KILOPASCAL);
		Quantity<Pressure> highPressureGauge = Quantities.getQuantity(request.highPressureGaugeKpa(), KILOPASCAL);
		Quantity<Length> altitude = Quantities.getQuantity(request.altitudeMeters(), Units.METRE);

		return new PressureTankInput(
				toCoreControlType(request.controlType()),
				drawdownFlowRate,
				request.minRunTimeMinutes(),
				lowPressureGauge,
				highPressureGauge,
				altitude);
	}

	public static PressureTankResponse toResponse(PressureTankResult result) {
		return new PressureTankResponse(
				result.drawdownVolume().to(Units.LITRE).getValue().doubleValue(),
				result.requiredTankVolume().to(Units.LITRE).getValue().doubleValue(),
				result.lowPressureAbsolute().to(KILOPASCAL).getValue().doubleValue(),
				result.highPressureAbsolute().to(KILOPASCAL).getValue().doubleValue());
	}

	private static PumpControlType toCoreControlType(PumpControlTypeDto dto) {
		return switch (dto) {
			case CONVENTIONAL -> PumpControlType.CONVENTIONAL;
			case VFD -> PumpControlType.VFD;
		};
	}

}
