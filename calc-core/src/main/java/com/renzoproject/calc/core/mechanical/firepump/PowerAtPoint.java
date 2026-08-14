package com.renzoproject.calc.core.mechanical.firepump;

import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;

import javax.measure.Quantity;
import javax.measure.quantity.Pressure;

/**
 * Horsepower fields are plain {@code double}, not Indriya quantities — deliberately, matching
 * this package's psi/GPM math convention: horsepower here is a derived NFPA/hydraulics
 * rule-of-thumb figure ({@code GPM * head / 3960}), not itself an SI-convertible physical unit
 * this codebase otherwise models with Indriya.
 */
public record PowerAtPoint(
		Quantity<VolumetricFlowRate> flow,
		Quantity<Pressure> pressure,
		double waterHorsepower,
		double brakeHorsepower) {

}
