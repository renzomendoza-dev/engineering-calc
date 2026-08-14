package com.renzoproject.calc_api.mechanical.pipe;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.DiameterSpec;
import com.renzoproject.calc.core.mechanical.pipe.DiameterSizingResult;
import com.renzoproject.calc.core.mechanical.pipe.PipeSizingMode;
import com.renzoproject.calc.core.mechanical.pipe.PipeSizingResult;
import com.renzoproject.calc.core.mechanical.pipe.PipeVelocityInput;
import com.renzoproject.calc.core.mechanical.pipe.VelocityResult;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.MetricPrefix;
import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import javax.measure.quantity.Speed;
import java.util.Map;

/**
 * Maps between calc-api's pipe velocity DTOs and calc-core's calculator types, including
 * parsing the request's {@code *Unit} strings into Indriya units and formatting the result's
 * quantities back out as fixed canonical units (mm, m/s — see {@link PipeVelocityResponse}).
 * Flow-rate/length unit parsing and {@code DiameterSpec} construction are shared with
 * {@link PipePressureLossMapper} via {@link PipeUnitParsing} rather than duplicated here.
 *
 * <p>Unrecognized unit strings throw {@link CalculationException} with a clear message listing
 * what's supported, same "controlled 400 instead of a raw deserialization failure" rationale as
 * {@code WireSizingMapper}'s {@code parseEnum} helper.
 */
public final class PipeVelocityMapper {

	private static final Unit<Length> MILLIMETRE = MetricPrefix.MILLI(Units.METRE);

	private static final Map<String, Unit<Speed>> SPEED_UNITS = Map.of(
			"m/s", Units.METRE_PER_SECOND,
			"ft/s", Units.METRE_PER_SECOND.multiply(0.3048));

	private PipeVelocityMapper() {
	}

	public static PipeVelocityInput toCoreInput(PipeVelocityRequest request) {
		Quantity<VolumetricFlowRate> flowRate = Quantities.getQuantity(request.flowRateValue(), PipeUnitParsing.parseFlowRateUnit(request.flowRateUnit()));
		PipeSizingMode mode = toCoreMode(request.mode());

		DiameterSpec diameterSpec = null;
		Quantity<Speed> targetVelocity = null;
		String pipeMaterial = null;
		String schedule = null;

		if (mode == PipeSizingMode.VELOCITY_FROM_DIAMETER) {
			diameterSpec = PipeUnitParsing.toCoreDiameterSpec(
					request.diameterSpecType(), request.nominalMaterial(), request.nominalSchedule(), request.nominalLabel(),
					request.rawDiameterValue(), request.rawDiameterUnit());
		} else {
			targetVelocity = Quantities.getQuantity(request.targetVelocityValue(), parseSpeedUnit(request.targetVelocityUnit()));
			pipeMaterial = request.pipeMaterial();
			schedule = request.schedule();
		}

		return new PipeVelocityInput(mode, flowRate, diameterSpec, targetVelocity, pipeMaterial, schedule);
	}

	public static PipeVelocityResponse toResponse(PipeSizingResult result) {
		return switch (result) {
			case VelocityResult velocity -> new PipeVelocityResponse(
					PipeSizingModeDto.VELOCITY_FROM_DIAMETER,
					velocity.velocity().to(Units.METRE_PER_SECOND).getValue().doubleValue(),
					"m/s",
					null, null, null, null, null, null, null);
			case DiameterSizingResult sizing -> new PipeVelocityResponse(
					PipeSizingModeDto.DIAMETER_FROM_VELOCITY,
					null, null,
					sizing.calculatedMinDiameter().to(MILLIMETRE).getValue().doubleValue(), "mm",
					sizing.nominalPipeSize(),
					sizing.actualInternalDiameter().to(MILLIMETRE).getValue().doubleValue(), "mm",
					sizing.actualVelocity().to(Units.METRE_PER_SECOND).getValue().doubleValue(), "m/s");
		};
	}

	private static PipeSizingMode toCoreMode(PipeSizingModeDto dto) {
		return switch (dto) {
			case VELOCITY_FROM_DIAMETER -> PipeSizingMode.VELOCITY_FROM_DIAMETER;
			case DIAMETER_FROM_VELOCITY -> PipeSizingMode.DIAMETER_FROM_VELOCITY;
		};
	}

	private static Unit<Speed> parseSpeedUnit(String rawValue) {
		Unit<Speed> unit = SPEED_UNITS.get(rawValue);
		if (unit == null) {
			throw new CalculationException("Unknown speed unit: " + rawValue + " (supported: " + SPEED_UNITS.keySet() + ")");
		}
		return unit;
	}

}
