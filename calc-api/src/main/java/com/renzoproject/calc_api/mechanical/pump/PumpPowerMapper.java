package com.renzoproject.calc_api.mechanical.pump;

import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;
import com.renzoproject.calc.core.mechanical.pump.PumpPowerInput;
import com.renzoproject.calc.core.mechanical.pump.PumpPowerResult;
import com.renzoproject.calc_api.mechanical.pipe.PipeUnitParsing;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Length;

/** Straightforward flat mapping — no sealed types or reference-data lookups involved. */
public final class PumpPowerMapper {

	private PumpPowerMapper() {
	}

	public static PumpPowerInput toCoreInput(PumpPowerRequest request) {
		Quantity<VolumetricFlowRate> flowRate = Quantities.getQuantity(request.flowRateValue(), PipeUnitParsing.parseFlowRateUnit(request.flowRateUnit()));
		Quantity<Length> totalDynamicHead = Quantities.getQuantity(request.totalDynamicHeadMeters(), Units.METRE);
		return new PumpPowerInput(flowRate, totalDynamicHead, request.pumpEfficiency(), request.fluidDensityKgM3());
	}

	public static PumpPowerResponse toResponse(PumpPowerResult result) {
		return new PumpPowerResponse(result.hydraulicPowerKw(), result.shaftPowerKw(), result.recommendedMotorSizeKw());
	}

}
