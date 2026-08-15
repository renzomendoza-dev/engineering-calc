package com.renzoproject.calc.core.mechanical.pump;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import java.util.List;

/**
 * @param staticallyFedWarning {@code true} if {@code totalDynamicHead <= 0} — never thrown as
 *                             an error; see {@link PumpTDHCalculator}'s Javadoc for why a
 *                             non-positive TDH is a legitimate result here, not an invalid input
 */
public record PumpTDHResult(
		Quantity<Length> staticHead,
		Quantity<Length> totalSuctionHeadLoss,
		Quantity<Length> totalDischargeHeadLoss,
		Quantity<Length> residualPressureHead,
		Quantity<Length> velocityHead,
		Quantity<Length> totalDynamicHead,
		boolean staticallyFedWarning,
		List<SegmentLossDetail> suctionSegmentDetails,
		List<SegmentLossDetail> dischargeSegmentDetails) {

}
