package com.renzoproject.calc.core.mechanical.firepump;

import javax.measure.Quantity;
import javax.measure.quantity.Pressure;

/**
 * @param meetsCapacityDemand candidate's rated flow AND rated pressure are both {@code >=} the
 *                            demand inputs
 * @param churnPressure       interpolated pressure at 0 GPM (shutoff/churn head)
 * @param churnCompliant      {@code churnPressure <=} rated pressure times the loaded churn
 *                            ceiling percentage
 * @param overloadPressure    interpolated pressure at the loaded overload flow percentage of
 *                            rated flow
 * @param overloadCompliant   {@code overloadPressure >=} rated pressure times the loaded
 *                            overload pressure floor percentage
 * @param overallCompliant    {@code true} only if all three of the above are {@code true}
 */
public record FirePumpCurveValidationResult(
		boolean meetsCapacityDemand,
		Quantity<Pressure> churnPressure,
		boolean churnCompliant,
		Quantity<Pressure> overloadPressure,
		boolean overloadCompliant,
		boolean overallCompliant) {

}
