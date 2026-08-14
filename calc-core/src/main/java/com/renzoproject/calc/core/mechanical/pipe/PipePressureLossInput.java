package com.renzoproject.calc.core.mechanical.pipe;

import com.renzoproject.calc.core.exception.CalculationException;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Temperature;

/**
 * Input for {@code PipePressureLossCalculator}.
 *
 * @param fluidKey         e.g. {@code "WATER"}, resolved via {@link FluidPropertiesResolver}
 * @param fluidTemperature must fall within the resolver's reference table range; this is
 *                         surfaced by the resolver itself as a {@link CalculationException},
 *                         not re-checked here (same "let the resolver own its own reference-data
 *                         range" reasoning as {@code PipeDimensionResolver}'s callers)
 * @param flowRate         must be positive
 * @param diameterSpec     reused directly from {@code PipeVelocityCalculator}'s package — a
 *                         {@link NominalSize} carries the material needed for the friction-factor
 *                         roughness lookup; a {@link RawDiameter} does not, and is only usable
 *                         with this calculator when the flow turns out to be LAMINAR (see
 *                         {@code PipePressureLossCalculator}'s Javadoc)
 * @param pipeLength       straight-run length; must be positive
 * @param method           ignored for laminar flow, where {@code f = 64/Re} always applies
 *                         regardless of which method was requested
 * @throws CalculationException if any validation rule above is violated
 */
public record PipePressureLossInput(
		String fluidKey,
		Quantity<Temperature> fluidTemperature,
		Quantity<VolumetricFlowRate> flowRate,
		DiameterSpec diameterSpec,
		Quantity<Length> pipeLength,
		FrictionFactorMethod method) {

	public PipePressureLossInput {
		if (fluidKey == null || fluidKey.isBlank()) {
			throw new CalculationException("fluidKey is required");
		}
		if (fluidTemperature == null) {
			throw new CalculationException("fluidTemperature is required");
		}
		if (flowRate == null) {
			throw new CalculationException("flowRate is required");
		}
		if (flowRate.getValue().doubleValue() <= 0) {
			throw new CalculationException("flowRate must be positive");
		}
		if (diameterSpec == null) {
			throw new CalculationException("diameterSpec is required");
		}
		if (diameterSpec instanceof RawDiameter raw
				&& (raw.internalDiameter() == null || raw.internalDiameter().getValue().doubleValue() <= 0)) {
			throw new CalculationException("diameterSpec's internalDiameter must be positive");
		}
		if (pipeLength == null) {
			throw new CalculationException("pipeLength is required");
		}
		if (pipeLength.getValue().doubleValue() <= 0) {
			throw new CalculationException("pipeLength must be positive");
		}
		if (method == null) {
			throw new CalculationException("method is required");
		}
	}

}
