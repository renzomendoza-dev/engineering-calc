package com.renzoproject.calc.core.mechanical.pump;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.DiameterSpec;
import com.renzoproject.calc.core.mechanical.pipe.FrictionFactorMethod;

import javax.measure.Quantity;
import javax.measure.quantity.Length;

/**
 * One straight run of pipe in a suction or discharge line — the same {@code DiameterSpec}/
 * {@code FrictionFactorMethod} types {@code PipePressureLossCalculator} already uses, reused
 * directly rather than redefined.
 */
public record PipeSegmentSpec(DiameterSpec diameterSpec, Quantity<Length> length, FrictionFactorMethod method) {

	public PipeSegmentSpec {
		if (diameterSpec == null) {
			throw new CalculationException("diameterSpec is required for a pipe segment");
		}
		if (length == null) {
			throw new CalculationException("length is required for a pipe segment");
		}
		if (method == null) {
			throw new CalculationException("method is required for a pipe segment");
		}
	}

}
