package com.renzoproject.calc_api.mechanical.pipe;

import com.renzoproject.calc.core.mechanical.pipe.DiameterSpec;
import com.renzoproject.calc.core.mechanical.pipe.FlowRegime;
import com.renzoproject.calc.core.mechanical.pipe.FrictionFactorMethod;
import com.renzoproject.calc.core.mechanical.pipe.PipePressureLossInput;
import com.renzoproject.calc.core.mechanical.pipe.PipePressureLossResult;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Temperature;

/**
 * Maps between calc-api's pipe pressure loss DTOs and calc-core's calculator types.
 * {@code DiameterSpec} construction and {@code flowRateUnit} parsing are shared with
 * {@link PipeVelocityMapper} via {@link PipeUnitParsing}, not duplicated here.
 */
public final class PipePressureLossMapper {

	private PipePressureLossMapper() {
	}

	public static PipePressureLossInput toCoreInput(PipePressureLossRequest request) {
		Quantity<VolumetricFlowRate> flowRate = Quantities.getQuantity(request.flowRateValue(), PipeUnitParsing.parseFlowRateUnit(request.flowRateUnit()));
		Quantity<Temperature> fluidTemperature = Quantities.getQuantity(request.fluidTemperatureCelsius(), Units.CELSIUS);
		DiameterSpec diameterSpec = PipeUnitParsing.toCoreDiameterSpec(
				request.diameterSpecType(), request.nominalMaterial(), request.nominalSchedule(), request.nominalLabel(),
				request.rawDiameterValue(), request.rawDiameterUnit());
		Quantity<Length> pipeLength = Quantities.getQuantity(request.pipeLengthMeters(), Units.METRE);
		FrictionFactorMethod method = toCoreMethod(request.method());

		return new PipePressureLossInput(request.fluidKey(), fluidTemperature, flowRate, diameterSpec, pipeLength, method);
	}

	public static PipePressureLossResponse toResponse(PipePressureLossResult result) {
		return new PipePressureLossResponse(
				result.velocity().to(Units.METRE_PER_SECOND).getValue().doubleValue(),
				result.reynoldsNumber(),
				toDtoFlowRegime(result.flowRegime()),
				result.transitionalRegimeWarning(),
				result.frictionFactor(),
				result.headLoss().to(Units.METRE).getValue().doubleValue(),
				result.pressureLoss().to(Units.PASCAL).getValue().doubleValue());
	}

	private static FrictionFactorMethod toCoreMethod(FrictionFactorMethodDto dto) {
		return switch (dto) {
			case COLEBROOK_WHITE -> FrictionFactorMethod.COLEBROOK_WHITE;
			case SWAMEE_JAIN -> FrictionFactorMethod.SWAMEE_JAIN;
		};
	}

	private static FlowRegimeDto toDtoFlowRegime(FlowRegime regime) {
		return switch (regime) {
			case LAMINAR -> FlowRegimeDto.LAMINAR;
			case TRANSITIONAL -> FlowRegimeDto.TRANSITIONAL;
			case TURBULENT -> FlowRegimeDto.TURBULENT;
		};
	}

}
