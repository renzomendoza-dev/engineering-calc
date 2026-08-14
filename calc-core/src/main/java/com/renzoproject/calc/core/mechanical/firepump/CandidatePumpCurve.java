package com.renzoproject.calc.core.mechanical.firepump;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;

import javax.measure.Quantity;
import javax.measure.quantity.Pressure;
import java.util.List;

/**
 * @param curvePoints must bracket both 0% and >= 150% of {@code ratedFlow} for
 *                    {@link FirePumpCurveValidationCalculator}/{@link FirePumpPowerCalculator}'s
 *                    {@code FullCurve} mode to evaluate successfully. Deliberately NOT validated
 *                    here at construction: the exact overload percentage (150% is NFPA 20's
 *                    current figure, but this package loads it from
 *                    {@code curve-requirements.json} rather than hardcoding it) isn't available
 *                    to a plain data record — only {@link PumpCurveInterpolator}, called with
 *                    the actually-loaded threshold, can check this. It throws
 *                    {@link CalculationException} at that point instead if the curve doesn't
 *                    span far enough, rather than extrapolating.
 */
public record CandidatePumpCurve(
		Quantity<VolumetricFlowRate> ratedFlow,
		Quantity<Pressure> ratedPressure,
		List<PumpCurvePoint> curvePoints) {

	public CandidatePumpCurve {
		if (ratedFlow == null) {
			throw new CalculationException("ratedFlow is required");
		}
		if (ratedPressure == null) {
			throw new CalculationException("ratedPressure is required");
		}
		if (curvePoints == null || curvePoints.size() < 2) {
			throw new CalculationException("curvePoints must contain at least two points");
		}
		curvePoints = List.copyOf(curvePoints);
	}

}
