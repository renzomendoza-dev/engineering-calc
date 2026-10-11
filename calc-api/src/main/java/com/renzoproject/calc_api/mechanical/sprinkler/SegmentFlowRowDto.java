package com.renzoproject.calc_api.mechanical.sprinkler;

import com.renzoproject.calc.core.mechanical.sprinkler.SegmentFlowRow;

/**
 * One row of the hydraulic calculation sheet, mirroring calc-core's {@link SegmentFlowRow}.
 * <b>Units:</b> psi, gpm, ft, in, fps.
 */
public record SegmentFlowRowDto(
		String fromNodeId,
		String toNodeId,
		Double flowGpm,
		String nominalSize,
		Double insideDiameterInch,
		Double cFactor,
		Double pipeLengthFt,
		Double fittingEquivalentLengthFt,
		Double fittingCFactorAdjustment,
		Double totalLengthFt,
		Double frictionLossPsiPerFoot,
		Double frictionLossPsi,
		Double elevationPressurePsi,
		Double endPressurePsi,
		Double velocityFps) {

	public static SegmentFlowRowDto from(SegmentFlowRow row) {
		return new SegmentFlowRowDto(
				row.fromNodeId(),
				row.toNodeId(),
				row.flowGpm(),
				row.nominalSize(),
				row.insideDiameterInch(),
				row.cFactor(),
				row.pipeLengthFt(),
				row.fittingEquivalentLengthFt(),
				row.fittingCFactorAdjustment(),
				row.totalLengthFt(),
				row.frictionLossPsiPerFoot(),
				row.frictionLossPsi(),
				row.elevationPressurePsi(),
				row.endPressurePsi(),
				row.velocityFps());
	}

}
