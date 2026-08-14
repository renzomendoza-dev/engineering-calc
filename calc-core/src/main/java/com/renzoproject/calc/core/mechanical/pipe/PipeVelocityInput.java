package com.renzoproject.calc.core.mechanical.pipe;

import com.renzoproject.calc.core.exception.CalculationException;

import javax.measure.Quantity;
import javax.measure.quantity.Speed;

/**
 * Input for {@link PipeVelocityCalculator}.
 *
 * @param mode           which quantity to solve for
 * @param flowRate       volumetric flow rate; must be positive
 * @param diameterSpec   required for {@link PipeSizingMode#VELOCITY_FROM_DIAMETER}; ignored for
 *                       {@link PipeSizingMode#DIAMETER_FROM_VELOCITY}
 * @param targetVelocity required and must be positive for
 *                       {@link PipeSizingMode#DIAMETER_FROM_VELOCITY}; ignored for
 *                       {@link PipeSizingMode#VELOCITY_FROM_DIAMETER}
 * @param pipeMaterial   required for {@link PipeSizingMode#DIAMETER_FROM_VELOCITY} — resolves
 *                       the *output* nominal size. Distinct from a {@link NominalSize}
 *                       diameterSpec's own {@code material}, which resolves the *input*
 *                       diameter in analysis mode instead.
 * @param schedule       optional; materials that publish more than one schedule/PN-rating
 *                       effectively require it — {@link PipeDimensionResolver} throws rather
 *                       than guessing if it's omitted and the lookup is ambiguous.
 * @throws CalculationException if any validation rule above is violated
 */
public record PipeVelocityInput(
		PipeSizingMode mode,
		Quantity<VolumetricFlowRate> flowRate,
		DiameterSpec diameterSpec,
		Quantity<Speed> targetVelocity,
		String pipeMaterial,
		String schedule) {

	public PipeVelocityInput {
		if (mode == null) {
			throw new CalculationException("mode is required");
		}
		if (flowRate == null) {
			throw new CalculationException("flowRate is required");
		}
		if (flowRate.getValue().doubleValue() <= 0) {
			throw new CalculationException("flowRate must be positive");
		}

		if (mode == PipeSizingMode.VELOCITY_FROM_DIAMETER) {
			if (diameterSpec == null) {
				throw new CalculationException("diameterSpec is required for VELOCITY_FROM_DIAMETER mode");
			}
			if (diameterSpec instanceof RawDiameter raw
					&& (raw.internalDiameter() == null || raw.internalDiameter().getValue().doubleValue() <= 0)) {
				throw new CalculationException("diameterSpec's internalDiameter must be positive");
			}
		} else {
			if (targetVelocity == null) {
				throw new CalculationException("targetVelocity is required for DIAMETER_FROM_VELOCITY mode");
			}
			if (targetVelocity.getValue().doubleValue() <= 0) {
				throw new CalculationException("targetVelocity must be positive");
			}
			if (pipeMaterial == null || pipeMaterial.isBlank()) {
				throw new CalculationException("pipeMaterial is required for DIAMETER_FROM_VELOCITY mode");
			}
		}
	}

}
