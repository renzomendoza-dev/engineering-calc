package com.renzoproject.calc.core.mechanical.pump;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Temperature;
import java.util.List;

/**
 * Input for {@link PumpTDHCalculator}.
 *
 * @param suctionSegments    may be empty (e.g. a pump sitting directly in a flooded wet well
 *                           with no suction piping) but not {@code null}
 * @param dischargeSegments  may be empty but not {@code null}
 * @param includeVelocityHead defaults to {@code false} in spirit — there's no such thing as an
 *                           omitted record component, so callers who don't want it pass
 *                           {@code false} explicitly. When {@code true}, requires at least one
 *                           discharge segment (see {@link PumpTDHCalculator})
 * @throws CalculationException if any validation rule below is violated
 */
public record PumpTDHInput(
		String fluidKey,
		Quantity<Temperature> fluidTemperature,
		Quantity<VolumetricFlowRate> flowRate,

		SuctionCondition suctionCondition,
		Quantity<Length> staticSuctionLift,
		Quantity<Length> staticSuctionHeadFlooded,
		List<PipeSegmentSpec> suctionSegments,

		Quantity<Length> staticDischargeElevation,
		Quantity<Pressure> requiredResidualPressure,
		List<PipeSegmentSpec> dischargeSegments,

		boolean includeVelocityHead) {

	public PumpTDHInput {
		if (fluidKey == null || fluidKey.isBlank()) {
			throw new CalculationException("fluidKey is required");
		}
		if (fluidTemperature == null) {
			throw new CalculationException("fluidTemperature is required");
		}
		if (flowRate == null) {
			throw new CalculationException("flowRate is required");
		}
		if (flowRate.getValue().doubleValue() <= 0) {
			throw new CalculationException("flowRate must be positive");
		}
		if (suctionCondition == null) {
			throw new CalculationException("suctionCondition is required");
		}

		if (suctionCondition == SuctionCondition.FLOODED) {
			if (staticSuctionHeadFlooded == null) {
				throw new CalculationException("staticSuctionHeadFlooded is required when suctionCondition is FLOODED");
			}
			if (staticSuctionLift != null) {
				throw new CalculationException("staticSuctionLift must be null when suctionCondition is FLOODED");
			}
		} else {
			if (staticSuctionLift == null) {
				throw new CalculationException("staticSuctionLift is required when suctionCondition is LIFT");
			}
			if (staticSuctionHeadFlooded != null) {
				throw new CalculationException("staticSuctionHeadFlooded must be null when suctionCondition is LIFT");
			}
		}

		if (suctionSegments == null) {
			throw new CalculationException("suctionSegments is required (pass an empty list, not null)");
		}
		if (staticDischargeElevation == null) {
			throw new CalculationException("staticDischargeElevation is required");
		}
		if (requiredResidualPressure == null) {
			throw new CalculationException("requiredResidualPressure is required");
		}
		if (dischargeSegments == null) {
			throw new CalculationException("dischargeSegments is required (pass an empty list, not null)");
		}

		suctionSegments = List.copyOf(suctionSegments);
		dischargeSegments = List.copyOf(dischargeSegments);
	}

}
