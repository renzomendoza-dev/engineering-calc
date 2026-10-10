package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * A fixed pressure loss through a device on the supply side (backflow preventer, alarm valve,
 * strainer, meter). Added to the required pressure before it is compared against the supply.
 *
 * @param name            device label, for the result's readout
 * @param pressureLossPsi loss at the system's demand flow; must not be negative
 * @throws CalculationException if any rule above is violated
 */
public record DeviceLoss(String name, double pressureLossPsi) {

	public DeviceLoss {
		if (name == null || name.isBlank()) {
			throw new CalculationException("device loss name is required");
		}
		if (pressureLossPsi < 0) {
			throw new CalculationException("pressureLossPsi must not be negative for device " + name);
		}
	}

}
