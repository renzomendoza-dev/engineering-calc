package com.renzoproject.calc.core.mechanical.pipe;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.exception.CalculationException;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Length;

/**
 * Pure pipe-flow geometry: {@code A = (pi/4) * D^2}, {@code V = Q/A}. No fluid properties or
 * friction/pressure-loss math here — that's a separate, later calculator.
 *
 * <p>No reference-data lookups happen in this class; both {@link NominalSize} diameter specs
 * (analysis mode) and the rounded-up output size (design mode) are resolved externally via
 * {@link PipeDimensionResolver}, matching the separation already established between
 * calculators and lookup tables elsewhere in this codebase.
 *
 * <p>Internally, all math is done in plain SI-base doubles (metres, m3/s, m/s) after converting
 * the Indriya inputs once — the same "raw doubles for computation" style every other calculator
 * in this codebase already uses — then the result is wrapped back into Indriya quantities at
 * the end. This keeps the actual arithmetic simple and predictable rather than relying on
 * quantity-arithmetic operators throughout.
 */
public class PipeVelocityCalculator implements Calculator<PipeVelocityInput, PipeSizingResult> {

	/**
	 * Floating-point tolerance for the design-mode sanity check that actual velocity (recomputed
	 * from the resolver's rounded-up size) never exceeds the requested target velocity. A plain
	 * Java {@code assert} isn't used for this because assertions are disabled by default at
	 * runtime (no {@code -ea}) — this check must always run, since it's guarding against a
	 * broken resolver silently returning an undersized pipe.
	 */
	private static final double VELOCITY_TOLERANCE_MPS = 1e-9;

	private final PipeDimensionResolver dimensionResolver;

	public PipeVelocityCalculator(PipeDimensionResolver dimensionResolver) {
		this.dimensionResolver = dimensionResolver;
	}

	@Override
	public PipeSizingResult calculate(PipeVelocityInput input) {
		double flowRateM3s = input.flowRate().to(PipeUnits.CUBIC_METRE_PER_SECOND).getValue().doubleValue();

		return switch (input.mode()) {
			case VELOCITY_FROM_DIAMETER -> calculateVelocityFromDiameter(input, flowRateM3s);
			case DIAMETER_FROM_VELOCITY -> calculateDiameterFromVelocity(input, flowRateM3s);
		};
	}

	private VelocityResult calculateVelocityFromDiameter(PipeVelocityInput input, double flowRateM3s) {
		Quantity<Length> internalDiameter = DiameterSpecResolver.resolveInternalDiameter(input.diameterSpec(), dimensionResolver);
		double diameterM = internalDiameter.to(Units.METRE).getValue().doubleValue();
		double velocityMs = velocityFor(flowRateM3s, diameterM);
		return new VelocityResult(Quantities.getQuantity(velocityMs, Units.METRE_PER_SECOND));
	}

	private DiameterSizingResult calculateDiameterFromVelocity(PipeVelocityInput input, double flowRateM3s) {
		double targetVelocityMs = input.targetVelocity().to(Units.METRE_PER_SECOND).getValue().doubleValue();
		double minDiameterM = Math.sqrt((4 * flowRateM3s) / (Math.PI * targetVelocityMs));
		Quantity<Length> calculatedMinDiameter = Quantities.getQuantity(minDiameterM, Units.METRE);

		PipeDimension resolved = dimensionResolver.resolveNextStandardSize(calculatedMinDiameter, input.pipeMaterial(), input.schedule());

		double actualDiameterM = resolved.internalDiameter().to(Units.METRE).getValue().doubleValue();
		double actualVelocityMs = velocityFor(flowRateM3s, actualDiameterM);

		if (actualVelocityMs > targetVelocityMs + VELOCITY_TOLERANCE_MPS) {
			throw new CalculationException("Resolved nominal size " + resolved.nominalLabel()
					+ " produces velocity " + actualVelocityMs + " m/s, which exceeds target velocity "
					+ targetVelocityMs + " m/s -- PipeDimensionResolver returned a size smaller than "
					+ "the calculated minimum diameter, which should never happen");
		}

		return new DiameterSizingResult(
				calculatedMinDiameter,
				resolved.nominalLabel(),
				resolved.internalDiameter(),
				Quantities.getQuantity(actualVelocityMs, Units.METRE_PER_SECOND));
	}

	private static double velocityFor(double flowRateM3s, double diameterM) {
		double area = (Math.PI / 4.0) * diameterM * diameterM;
		return flowRateM3s / area;
	}

}
