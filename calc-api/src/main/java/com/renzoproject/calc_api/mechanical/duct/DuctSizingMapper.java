package com.renzoproject.calc_api.mechanical.duct;

import com.renzoproject.calc.core.mechanical.duct.DuctShape;
import com.renzoproject.calc.core.mechanical.duct.DuctSizingInput;
import com.renzoproject.calc.core.mechanical.duct.DuctSizingMethod;
import com.renzoproject.calc.core.mechanical.duct.DuctSizingResult;
import com.renzoproject.calc.core.mechanical.duct.FixedDimensionType;
import com.renzoproject.calc.core.mechanical.pipe.FrictionFactorMethod;
import com.renzoproject.calc.core.mechanical.pipe.PipeUnits;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;
import com.renzoproject.calc_api.mechanical.pipe.FrictionFactorMethodDto;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.MetricPrefix;
import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Speed;
import javax.measure.quantity.Temperature;

/**
 * Pure mapping, no logic -- matches every other mapper in this codebase. All mm/L-s/m-s
 * boundary conversions happen here: calc-core stays unit-convention-pure internally (metres,
 * m3/s), this mapper is the one place that knows this endpoint's DTOs are HVAC-industry mixed
 * units.
 *
 * <p>{@code targetFrictionRatePaPerM}/{@code actualFrictionRatePaPerM} map to/from calc-core's
 * {@code Quantity<Pressure>} fields as plain Pascals (not a distinct "pressure per length"
 * quantity type) -- that's the type calc-core's {@code DuctSizingInput}/{@code DuctSizingResult}
 * already use for this value, so this mapper matches that existing contract rather than
 * introducing a mismatched conversion of its own.
 */
public final class DuctSizingMapper {

	private static final Unit<Length> MILLIMETRE = MetricPrefix.MILLI(Units.METRE);

	private DuctSizingMapper() {
	}

	public static DuctSizingInput toCoreInput(DuctSizingRequest request) {
		Quantity<VolumetricFlowRate> airFlow = Quantities.getQuantity(request.airFlowLps(), PipeUnits.LITRE_PER_SECOND);
		Quantity<Temperature> airTemperature = Quantities.getQuantity(request.airTemperatureCelsius(), Units.CELSIUS);
		Quantity<Length> altitude = Quantities.getQuantity(request.altitudeMeters(), Units.METRE);
		FrictionFactorMethod frictionMethod = toCoreFrictionMethod(request.frictionMethod());
		DuctShape shape = toCoreShape(request.shape());
		DuctSizingMethod method = toCoreMethod(request.method());
		FixedDimensionType fixedDimensionType = toCoreFixedDimensionType(request.fixedDimensionType());

		Quantity<Length> fixedDimensionValue = request.fixedDimensionValueMm() == null
				? null
				: Quantities.getQuantity(request.fixedDimensionValueMm(), MILLIMETRE);
		Quantity<Pressure> targetFrictionRatePerMeter = request.targetFrictionRatePaPerM() == null
				? null
				: Quantities.getQuantity(request.targetFrictionRatePaPerM(), Units.PASCAL);
		Quantity<Speed> maxVelocity = request.maxVelocityMps() == null
				? null
				: Quantities.getQuantity(request.maxVelocityMps(), Units.METRE_PER_SECOND);

		return new DuctSizingInput(
				method, airFlow, airTemperature, altitude, request.ductMaterial(), frictionMethod,
				shape, fixedDimensionType, fixedDimensionValue, targetFrictionRatePerMeter, maxVelocity);
	}

	public static DuctSizingResponse toResponse(DuctSizingResult result) {
		Double widthMm = result.ductWidth() == null ? null : result.ductWidth().to(MILLIMETRE).getValue().doubleValue();
		Double heightMm = result.ductHeight() == null ? null : result.ductHeight().to(MILLIMETRE).getValue().doubleValue();

		return new DuctSizingResponse(
				result.equivalentDiameter().to(MILLIMETRE).getValue().doubleValue(),
				widthMm,
				heightMm,
				result.actualVelocity().to(Units.METRE_PER_SECOND).getValue().doubleValue(),
				result.reynoldsNumber(),
				result.frictionFactor(),
				result.actualFrictionRatePerMeter().to(Units.PASCAL).getValue().doubleValue());
	}

	private static DuctSizingMethod toCoreMethod(DuctSizingMethodDto dto) {
		return switch (dto) {
			case EQUAL_FRICTION -> DuctSizingMethod.EQUAL_FRICTION;
			case VELOCITY -> DuctSizingMethod.VELOCITY;
		};
	}

	private static DuctShape toCoreShape(DuctShapeDto dto) {
		return switch (dto) {
			case ROUND -> DuctShape.ROUND;
			case RECTANGULAR -> DuctShape.RECTANGULAR;
		};
	}

	private static FixedDimensionType toCoreFixedDimensionType(FixedDimensionTypeDto dto) {
		if (dto == null) {
			return null;
		}
		return switch (dto) {
			case WIDTH -> FixedDimensionType.WIDTH;
			case HEIGHT -> FixedDimensionType.HEIGHT;
		};
	}

	private static FrictionFactorMethod toCoreFrictionMethod(FrictionFactorMethodDto dto) {
		return switch (dto) {
			case COLEBROOK_WHITE -> FrictionFactorMethod.COLEBROOK_WHITE;
			case SWAMEE_JAIN -> FrictionFactorMethod.SWAMEE_JAIN;
		};
	}

}
