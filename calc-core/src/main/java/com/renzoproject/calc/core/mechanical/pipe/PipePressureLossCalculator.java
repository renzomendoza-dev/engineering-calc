package com.renzoproject.calc.core.mechanical.pipe;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.exception.CalculationException;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Length;

/**
 * Darcy-Weisbach straight-pipe friction loss, generalized across fluids and both
 * Colebrook-White/Swamee-Jain friction-factor correlations. SI units throughout (metres, m/s,
 * Pa, kg/m3) — unlike {@code mechanical.firepump}, which deliberately works in psi/GPM.
 *
 * <p>Minor losses (fittings, valves, elbows) are explicitly out of scope — straight-pipe
 * friction only.
 *
 * <p><b>{@link RawDiameter} + non-laminar flow doesn't work.</b> The friction factor for
 * TURBULENT/TRANSITIONAL flow needs the pipe's absolute roughness, which
 * {@link PipeRoughnessResolver} looks up by material — but {@link RawDiameter} carries no
 * material (it's a bare length, the "custom pipe" escape hatch). A {@link NominalSize}
 * diameterSpec always works, since it carries {@code material()}. This wasn't spelled out in
 * this calculator's original input record shape (which only has a {@code diameterSpec}, not a
 * separate material field), so rather than silently guessing a roughness value, a
 * {@link RawDiameter} combined with anything other than LAMINAR flow throws a clear
 * {@link CalculationException} instead.
 */
public class PipePressureLossCalculator implements Calculator<PipePressureLossInput, PipePressureLossResult> {

	private static final double GRAVITY_M_S2 = 9.80665;
	private static final double LAMINAR_UPPER_RE = 2300.0;
	private static final double TURBULENT_LOWER_RE = 4000.0;
	private static final double CONVERGENCE_RELATIVE_TOLERANCE = 1e-6;
	private static final int MAX_COLEBROOK_ITERATIONS = 50;

	private final PipeDimensionResolver dimensionResolver;
	private final FluidPropertiesResolver fluidPropertiesResolver;
	private final PipeRoughnessResolver roughnessResolver;

	public PipePressureLossCalculator(
			PipeDimensionResolver dimensionResolver,
			FluidPropertiesResolver fluidPropertiesResolver,
			PipeRoughnessResolver roughnessResolver) {
		this.dimensionResolver = dimensionResolver;
		this.fluidPropertiesResolver = fluidPropertiesResolver;
		this.roughnessResolver = roughnessResolver;
	}

	@Override
	public PipePressureLossResult calculate(PipePressureLossInput input) {
		Quantity<Length> internalDiameter = DiameterSpecResolver.resolveInternalDiameter(input.diameterSpec(), dimensionResolver);
		double diameterM = internalDiameter.to(Units.METRE).getValue().doubleValue();
		if (diameterM <= 0) {
			throw new CalculationException("Resolved internal diameter must be positive, was " + diameterM + " m");
		}

		FluidProperties fluid = fluidPropertiesResolver.resolve(input.fluidKey(), input.fluidTemperature());

		double flowRateM3s = input.flowRate().to(PipeUnits.CUBIC_METRE_PER_SECOND).getValue().doubleValue();
		double area = (Math.PI / 4.0) * diameterM * diameterM;
		double velocityMs = flowRateM3s / area;

		double reynoldsNumber = (fluid.densityKgM3() * velocityMs * diameterM) / fluid.dynamicViscosityPaS();

		FlowRegime flowRegime;
		boolean transitionalRegimeWarning;
		if (reynoldsNumber < LAMINAR_UPPER_RE) {
			flowRegime = FlowRegime.LAMINAR;
			transitionalRegimeWarning = false;
		} else if (reynoldsNumber >= TURBULENT_LOWER_RE) {
			flowRegime = FlowRegime.TURBULENT;
			transitionalRegimeWarning = false;
		} else {
			flowRegime = FlowRegime.TRANSITIONAL;
			transitionalRegimeWarning = true;
		}

		double frictionFactor = flowRegime == FlowRegime.LAMINAR
				? 64.0 / reynoldsNumber
				: turbulentFrictionFactor(input, diameterM, reynoldsNumber);

		double pipeLengthM = input.pipeLength().to(Units.METRE).getValue().doubleValue();
		double headLossM = frictionFactor * (pipeLengthM / diameterM) * (velocityMs * velocityMs / (2 * GRAVITY_M_S2));
		double pressureLossPa = frictionFactor * (pipeLengthM / diameterM) * (fluid.densityKgM3() * velocityMs * velocityMs / 2.0);

		return new PipePressureLossResult(
				Quantities.getQuantity(velocityMs, Units.METRE_PER_SECOND),
				reynoldsNumber,
				flowRegime,
				transitionalRegimeWarning,
				frictionFactor,
				Quantities.getQuantity(headLossM, Units.METRE),
				Quantities.getQuantity(pressureLossPa, Units.PASCAL));
	}

	/**
	 * Swamee-Jain is always computed first (it's explicit, and doubles as the Colebrook-White
	 * seed value when that method is requested), then refined via Colebrook-White if that's
	 * what {@code input.method()} asked for.
	 */
	private double turbulentFrictionFactor(PipePressureLossInput input, double diameterM, double reynoldsNumber) {
		String material = requireMaterialForRoughness(input.diameterSpec());
		double roughnessM = roughnessResolver.resolveAbsoluteRoughnessMm(material) / 1000.0;
		double relativeRoughness = roughnessM / diameterM;

		double swameeJain = swameeJainFrictionFactor(relativeRoughness, reynoldsNumber);
		return input.method() == FrictionFactorMethod.SWAMEE_JAIN
				? swameeJain
				: colebrookWhiteFrictionFactor(relativeRoughness, reynoldsNumber, swameeJain);
	}

	private static String requireMaterialForRoughness(DiameterSpec diameterSpec) {
		if (diameterSpec instanceof NominalSize nominal) {
			return nominal.material();
		}
		throw new CalculationException("Cannot resolve pipe roughness for a RawDiameter diameterSpec in "
				+ "TURBULENT/TRANSITIONAL flow -- the friction factor correlation needs the pipe material's "
				+ "absolute roughness, and RawDiameter carries no material. Use a NominalSize instead, or "
				+ "confirm the flow is actually LAMINAR (which doesn't need roughness at all).");
	}

	private static double swameeJainFrictionFactor(double relativeRoughness, double reynoldsNumber) {
		double term = relativeRoughness / 3.7 + 5.74 / Math.pow(reynoldsNumber, 0.9);
		double log10Term = Math.log10(term);
		return 0.25 / (log10Term * log10Term);
	}

	/**
	 * Standard fixed-point iteration on {@code x = 1/sqrt(f)}:
	 * {@code x_next = -2*log10(relativeRoughness/3.7 + 2.51*x/Re)} — converges reliably for this
	 * equation's shape (the textbook approach), unlike iterating on {@code f} directly.
	 */
	private static double colebrookWhiteFrictionFactor(double relativeRoughness, double reynoldsNumber, double initialGuess) {
		double x = 1.0 / Math.sqrt(initialGuess);
		for (int iteration = 0; iteration < MAX_COLEBROOK_ITERATIONS; iteration++) {
			double xNext = -2.0 * Math.log10(relativeRoughness / 3.7 + 2.51 * x / reynoldsNumber);
			double relativeChange = Math.abs(xNext - x) / Math.abs(x);
			x = xNext;
			if (relativeChange < CONVERGENCE_RELATIVE_TOLERANCE) {
				return 1.0 / (x * x);
			}
		}
		throw new CalculationException("Colebrook-White iteration did not converge within " + MAX_COLEBROOK_ITERATIONS
				+ " iterations (relative tolerance " + CONVERGENCE_RELATIVE_TOLERANCE + ")");
	}

}
