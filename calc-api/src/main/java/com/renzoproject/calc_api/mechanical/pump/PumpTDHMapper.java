package com.renzoproject.calc_api.mechanical.pump;

import com.renzoproject.calc.core.mechanical.pipe.DiameterSpec;
import com.renzoproject.calc.core.mechanical.pipe.FrictionFactorMethod;
import com.renzoproject.calc.core.mechanical.pipe.NominalSize;
import com.renzoproject.calc.core.mechanical.pipe.RawDiameter;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;
import com.renzoproject.calc.core.mechanical.pump.PipeSegmentSpec;
import com.renzoproject.calc.core.mechanical.pump.PumpTDHInput;
import com.renzoproject.calc.core.mechanical.pump.PumpTDHResult;
import com.renzoproject.calc.core.mechanical.pump.SegmentLossDetail;
import com.renzoproject.calc.core.mechanical.pump.SuctionCondition;
import com.renzoproject.calc_api.mechanical.pipe.DiameterSpecTypeDto;
import com.renzoproject.calc_api.mechanical.pipe.FrictionFactorMethodDto;
import com.renzoproject.calc_api.mechanical.pipe.PipePressureLossMapper;
import com.renzoproject.calc_api.mechanical.pipe.PipeUnitParsing;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.MetricPrefix;
import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Temperature;
import java.util.List;

/**
 * Maps between calc-api's pump TDH DTOs and calc-core's calculator types. Diameter-spec
 * construction and flow-rate unit parsing are shared with {@link PipePressureLossMapper}/
 * {@code PipeVelocityMapper} via {@link PipeUnitParsing} — this is the third caller of that
 * logic, consolidated there rather than duplicated a fourth time. Each segment's embedded
 * {@code PipePressureLossResponse} is built via {@link PipePressureLossMapper#toResponse}
 * directly, not remapped field-by-field.
 */
public final class PumpTDHMapper {

	private PumpTDHMapper() {
	}

	public static PumpTDHInput toCoreInput(PumpTDHRequest request) {
		Quantity<VolumetricFlowRate> flowRate = Quantities.getQuantity(request.flowRateValue(), PipeUnitParsing.parseFlowRateUnit(request.flowRateUnit()));
		Quantity<Temperature> fluidTemperature = Quantities.getQuantity(request.fluidTemperatureCelsius(), Units.CELSIUS);
		SuctionCondition suctionCondition = toCoreSuctionCondition(request.suctionCondition());

		Quantity<Length> staticSuctionLift = request.staticSuctionLiftMeters() == null
				? null : Quantities.getQuantity(request.staticSuctionLiftMeters(), Units.METRE);
		Quantity<Length> staticSuctionHeadFlooded = request.staticSuctionHeadFloodedMeters() == null
				? null : Quantities.getQuantity(request.staticSuctionHeadFloodedMeters(), Units.METRE);

		Quantity<Length> staticDischargeElevation = Quantities.getQuantity(request.staticDischargeElevationMeters(), Units.METRE);
		Quantity<Pressure> requiredResidualPressure = Quantities.getQuantity(request.requiredResidualPressureKpa(), MetricPrefix.KILO(Units.PASCAL));

		return new PumpTDHInput(
				request.fluidKey(), fluidTemperature, flowRate,
				suctionCondition, staticSuctionLift, staticSuctionHeadFlooded, toCoreSegments(request.suctionSegments()),
				staticDischargeElevation, requiredResidualPressure, toCoreSegments(request.dischargeSegments()),
				request.includeVelocityHead());
	}

	public static PumpTDHResponse toResponse(PumpTDHResult result) {
		return new PumpTDHResponse(
				result.staticHead().to(Units.METRE).getValue().doubleValue(),
				result.totalSuctionHeadLoss().to(Units.METRE).getValue().doubleValue(),
				result.totalDischargeHeadLoss().to(Units.METRE).getValue().doubleValue(),
				result.residualPressureHead().to(Units.METRE).getValue().doubleValue(),
				result.velocityHead().to(Units.METRE).getValue().doubleValue(),
				result.totalDynamicHead().to(Units.METRE).getValue().doubleValue(),
				result.staticallyFedWarning(),
				toDetailDtos(result.suctionSegmentDetails()),
				toDetailDtos(result.dischargeSegmentDetails()));
	}

	private static List<PipeSegmentSpec> toCoreSegments(List<PipeSegmentSpecDto> segments) {
		if (segments == null) {
			return null;
		}
		return segments.stream().map(PumpTDHMapper::toCoreSegment).toList();
	}

	private static PipeSegmentSpec toCoreSegment(PipeSegmentSpecDto dto) {
		DiameterSpec diameterSpec = PipeUnitParsing.toCoreDiameterSpec(
				dto.diameterSpecType(), dto.nominalMaterial(), dto.nominalSchedule(), dto.nominalLabel(),
				dto.rawDiameterValue(), dto.rawDiameterUnit());
		Quantity<Length> length = Quantities.getQuantity(dto.lengthMeters(), Units.METRE);
		return new PipeSegmentSpec(diameterSpec, length, toCoreMethod(dto.method()));
	}

	private static List<SegmentLossDetailDto> toDetailDtos(List<SegmentLossDetail> details) {
		return details.stream().map(PumpTDHMapper::toDetailDto).toList();
	}

	private static SegmentLossDetailDto toDetailDto(SegmentLossDetail detail) {
		return new SegmentLossDetailDto(toSegmentDto(detail.segment()), PipePressureLossMapper.toResponse(detail.result()));
	}

	private static PipeSegmentSpecDto toSegmentDto(PipeSegmentSpec segment) {
		double lengthMeters = segment.length().to(Units.METRE).getValue().doubleValue();
		FrictionFactorMethodDto method = toDtoMethod(segment.method());

		return switch (segment.diameterSpec()) {
			case NominalSize nominal -> new PipeSegmentSpecDto(
					DiameterSpecTypeDto.NOMINAL, nominal.material(), nominal.schedule(), nominal.nominalLabel(),
					null, null, lengthMeters, method);
			case RawDiameter raw -> new PipeSegmentSpecDto(
					DiameterSpecTypeDto.RAW, null, null, null,
					raw.internalDiameter().to(Units.METRE).getValue().doubleValue(), "m",
					lengthMeters, method);
		};
	}

	private static SuctionCondition toCoreSuctionCondition(SuctionConditionDto dto) {
		return switch (dto) {
			case FLOODED -> SuctionCondition.FLOODED;
			case LIFT -> SuctionCondition.LIFT;
		};
	}

	private static FrictionFactorMethod toCoreMethod(FrictionFactorMethodDto dto) {
		return switch (dto) {
			case COLEBROOK_WHITE -> FrictionFactorMethod.COLEBROOK_WHITE;
			case SWAMEE_JAIN -> FrictionFactorMethod.SWAMEE_JAIN;
		};
	}

	private static FrictionFactorMethodDto toDtoMethod(FrictionFactorMethod method) {
		return switch (method) {
			case COLEBROOK_WHITE -> FrictionFactorMethodDto.COLEBROOK_WHITE;
			case SWAMEE_JAIN -> FrictionFactorMethodDto.SWAMEE_JAIN;
		};
	}

}
