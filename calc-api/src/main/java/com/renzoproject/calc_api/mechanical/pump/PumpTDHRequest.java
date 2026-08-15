package com.renzoproject.calc_api.mechanical.pump;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * HTTP request body for a Total Dynamic Head calculation. SI units throughout, matching
 * calc-core's convention for {@code mechanical.pump}/{@code mechanical.pipe}.
 *
 * <p>{@code suctionSegments}/{@code dischargeSegments} may be empty lists but are deliberately
 * not annotated {@code @NotNull} here — a {@code null} list is left to calc-core's
 * {@code PumpTDHInput}, which already throws a clear {@code CalculationException} for it, same
 * "don't duplicate calc-core's own validation" reasoning used throughout this DTO family.
 *
 * <p>{@link #isSuctionFieldsValid()} enforces {@code suctionCondition}-dependent required
 * fields, same {@code @AssertTrue} pattern used elsewhere (e.g.
 * {@code FirePumpDemandRequest.isSuctionFieldsValid()}).
 */
public record PumpTDHRequest(
		@NotNull String fluidKey,
		@NotNull Double fluidTemperatureCelsius,
		@NotNull @Positive Double flowRateValue,
		@NotNull String flowRateUnit,

		@NotNull SuctionConditionDto suctionCondition,
		Double staticSuctionLiftMeters,
		Double staticSuctionHeadFloodedMeters,
		List<@Valid PipeSegmentSpecDto> suctionSegments,

		@NotNull Double staticDischargeElevationMeters,
		@NotNull Double requiredResidualPressureKpa,
		List<@Valid PipeSegmentSpecDto> dischargeSegments,

		boolean includeVelocityHead) {

	@AssertTrue(message = "When suctionCondition is FLOODED, staticSuctionHeadFloodedMeters is "
			+ "required and staticSuctionLiftMeters must be omitted. When LIFT, "
			+ "staticSuctionLiftMeters is required and staticSuctionHeadFloodedMeters must be omitted.")
	public boolean isSuctionFieldsValid() {
		if (suctionCondition == null) {
			return true; // let @NotNull on suctionCondition itself report this
		}
		return switch (suctionCondition) {
			case FLOODED -> staticSuctionHeadFloodedMeters != null && staticSuctionLiftMeters == null;
			case LIFT -> staticSuctionLiftMeters != null && staticSuctionHeadFloodedMeters == null;
		};
	}

}
