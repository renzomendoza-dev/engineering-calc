package com.renzoproject.calc.core.mechanical.firepump;

import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;

import javax.measure.Quantity;
import javax.measure.quantity.Pressure;

/**
 * @param ratedFlow     {@code requiredFlow} with the safety margin applied
 * @param ratedPressure net pressure the pump must produce at rated flow
 */
public record FirePumpDemandResult(Quantity<VolumetricFlowRate> ratedFlow, Quantity<Pressure> ratedPressure) {

}
