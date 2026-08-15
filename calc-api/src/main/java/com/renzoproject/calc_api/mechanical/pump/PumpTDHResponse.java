package com.renzoproject.calc_api.mechanical.pump;

import java.util.List;

public record PumpTDHResponse(
		Double staticHeadMeters,
		Double totalSuctionHeadLossMeters,
		Double totalDischargeHeadLossMeters,
		Double residualPressureHeadMeters,
		Double velocityHeadMeters,
		Double totalDynamicHeadMeters,
		boolean staticallyFedWarning,
		List<SegmentLossDetailDto> suctionSegmentDetails,
		List<SegmentLossDetailDto> dischargeSegmentDetails) {

}
