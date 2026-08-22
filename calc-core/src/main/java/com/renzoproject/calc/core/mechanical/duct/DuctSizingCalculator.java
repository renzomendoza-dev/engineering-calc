package com.renzoproject.calc.core.mechanical.duct;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.BisectionSolver;
import com.renzoproject.calc.core.mechanical.FrictionFactorCalculator;
import com.renzoproject.calc.core.mechanical.pipe.FluidProperties;
import com.renzoproject.calc.core.mechanical.pipe.FrictionFactorMethod;
import com.renzoproject.calc.core.mechanical.pipe.PipeUnits;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

/**
 * ASHRAE Fundamentals Ch.21 duct sizing: Equal Friction or Velocity method, round or rectangular
 * shape, temperature/altitude-corrected air properties.
 *
 * <p>Two nested iterative solves, both via the shared {@link BisectionSolver} rather than
 * bespoke loops:
 * <ul>
 *   <li><b>Round + EQUAL_FRICTION</b>: an outer bisection over diameter, wrapping an inner
 *       per-diameter friction-rate calculation that itself iterates when
 *       {@link FrictionFactorMethod#COLEBROOK_WHITE} is selected (via
 *       {@link FrictionFactorCalculator}). The outer bracket is centered on a velocity-based
 *       initial guess (assuming a generic 5 m/s) rather than seeding a Newton-Raphson step --
 *       bisection needs a bracket, not a single seed, and this guess is generous enough
 *       (expanded 10x each direction) to bracket realistic HVAC duct-sizing inputs without
 *       needing a derivative of the friction-rate function, which isn't available in closed form
 *       once Colebrook-White's own inner iteration is involved.</li>
 *   <li><b>Rectangular (either method)</b>: first computes the round-equivalent target diameter
 *       using the same round-sizing logic above, then a second bisection solves for the free
 *       dimension such that ASHRAE's equivalent-diameter formula matches that target.</li>
 * </ul>
 */
public class DuctSizingCalculator implements Calculator<DuctSizingInput, DuctSizingResult> {

	private static final double ASSUMED_SEED_VELOCITY_MS = 5.0;
	private static final double BRACKET_EXPANSION_FACTOR = 10.0;
	private static final double MIN_DIAMETER_M = 0.001;
	private static final double MAX_RECTANGULAR_SEARCH_BOUND_M = 10.0;
	private static final double MAX_RECTANGULAR_DIMENSION_M = 5.0;
	private static final double RELATIVE_TOLERANCE = 1e-6;
	private static final double LAMINAR_UPPER_RE = 2300.0;
	private static final String NO_DUCT_SIZE_MESSAGE =
			"No duct size satisfies these inputs -- try adjusting the target friction rate or fixed dimension.";

	private final AirPropertiesResolver airPropertiesResolver;
	private final DuctRoughnessResolver roughnessResolver;

	public DuctSizingCalculator(AirPropertiesResolver airPropertiesResolver, DuctRoughnessResolver roughnessResolver) {
		this.airPropertiesResolver = airPropertiesResolver;
		this.roughnessResolver = roughnessResolver;
	}

	@Override
	public DuctSizingResult calculate(DuctSizingInput input) {
		FluidProperties air = airPropertiesResolver.resolve(input.airTemperature(), input.altitude());
		double roughnessM = roughnessResolver.resolveAbsoluteRoughnessMm(input.ductMaterial()) / 1000.0;
		double flowRateM3s = input.airFlow().to(PipeUnits.CUBIC_METRE_PER_SECOND).getValue().doubleValue();

		double targetEquivalentDiameterM = switch (input.method()) {
			case VELOCITY -> diameterFromVelocity(flowRateM3s, input.maxVelocity().to(Units.METRE_PER_SECOND).getValue().doubleValue());
			case EQUAL_FRICTION -> solveEqualFrictionDiameter(
					flowRateM3s, input.targetFrictionRatePerMeter().to(Units.PASCAL).getValue().doubleValue(), air, roughnessM, input.frictionMethod());
		};

		return switch (input.shape()) {
			case ROUND -> roundResult(targetEquivalentDiameterM, flowRateM3s, air, roughnessM, input.frictionMethod());
			case RECTANGULAR -> rectangularResult(
					targetEquivalentDiameterM, flowRateM3s, air, roughnessM, input.frictionMethod(),
					input.fixedDimensionType(), input.fixedDimensionValue().to(Units.METRE).getValue().doubleValue());
		};
	}

	/** Identical formula to {@code PipeVelocityCalculator}'s {@code DIAMETER_FROM_VELOCITY} mode. */
	private static double diameterFromVelocity(double flowRateM3s, double velocityMs) {
		return Math.sqrt((4.0 * flowRateM3s) / (Math.PI * velocityMs));
	}

	private double solveEqualFrictionDiameter(
			double flowRateM3s, double targetFrictionRatePaPerM, FluidProperties air, double roughnessM, FrictionFactorMethod method) {
		double seedDiameterM = diameterFromVelocity(flowRateM3s, ASSUMED_SEED_VELOCITY_MS);
		double lowerBound = Math.max(MIN_DIAMETER_M, seedDiameterM / BRACKET_EXPANSION_FACTOR);
		double upperBound = seedDiameterM * BRACKET_EXPANSION_FACTOR;

		try {
			return BisectionSolver.solve(
					diameterM -> frictionRateAt(diameterM, flowRateM3s, air, roughnessM, method).frictionRatePaPerM() - targetFrictionRatePaPerM,
					lowerBound, upperBound, RELATIVE_TOLERANCE);
		} catch (CalculationException e) {
			throw new CalculationException(NO_DUCT_SIZE_MESSAGE, e);
		}
	}

	private DuctSizingResult roundResult(double diameterM, double flowRateM3s, FluidProperties air, double roughnessM, FrictionFactorMethod method) {
		FrictionRateAtDiameter fr = frictionRateAt(diameterM, flowRateM3s, air, roughnessM, method);
		return new DuctSizingResult(
				Quantities.getQuantity(diameterM, Units.METRE),
				null,
				null,
				Quantities.getQuantity(fr.velocityMs(), Units.METRE_PER_SECOND),
				fr.reynoldsNumber(),
				fr.frictionFactor(),
				Quantities.getQuantity(fr.frictionRatePaPerM(), Units.PASCAL));
	}

	private DuctSizingResult rectangularResult(
			double targetEquivalentDiameterM, double flowRateM3s, FluidProperties air, double roughnessM,
			FrictionFactorMethod method, FixedDimensionType fixedDimensionType, double fixedDimensionM) {
		double freeDimensionM = solveFreeDimension(targetEquivalentDiameterM, fixedDimensionM);

		double widthM = fixedDimensionType == FixedDimensionType.WIDTH ? fixedDimensionM : freeDimensionM;
		double heightM = fixedDimensionType == FixedDimensionType.HEIGHT ? fixedDimensionM : freeDimensionM;
		double actualEquivalentDiameterM = equivalentDiameterM(widthM, heightM);

		// The equivalent-diameter formula exists precisely so a round duct of this diameter,
		// carrying the SAME airFlow, has the same friction rate as the actual rectangular duct --
		// that's what makes reusing frictionRateAt() here correct, not an approximation.
		FrictionRateAtDiameter fr = frictionRateAt(actualEquivalentDiameterM, flowRateM3s, air, roughnessM, method);

		// actualVelocity is the TRUE physical velocity through the rectangular cross-section,
		// which differs from fr.velocityMs() (the velocity a round duct of actualEquivalentDiameterM
		// would have) -- the equivalent diameter is a friction-equivalent size, not a literal one.
		double actualVelocityMs = flowRateM3s / (widthM * heightM);

		return new DuctSizingResult(
				Quantities.getQuantity(actualEquivalentDiameterM, Units.METRE),
				Quantities.getQuantity(widthM, Units.METRE),
				Quantities.getQuantity(heightM, Units.METRE),
				Quantities.getQuantity(actualVelocityMs, Units.METRE_PER_SECOND),
				fr.reynoldsNumber(),
				fr.frictionFactor(),
				Quantities.getQuantity(fr.frictionRatePaPerM(), Units.PASCAL));
	}

	private double solveFreeDimension(double targetEquivalentDiameterM, double fixedDimensionM) {
		double freeDimensionM;
		try {
			freeDimensionM = BisectionSolver.solve(
					free -> equivalentDiameterM(free, fixedDimensionM) - targetEquivalentDiameterM,
					MIN_DIAMETER_M, MAX_RECTANGULAR_SEARCH_BOUND_M, RELATIVE_TOLERANCE);
		} catch (CalculationException e) {
			throw new CalculationException(NO_DUCT_SIZE_MESSAGE, e);
		}

		if (freeDimensionM <= 0 || freeDimensionM > MAX_RECTANGULAR_DIMENSION_M) {
			throw new CalculationException("Solved rectangular duct dimension " + freeDimensionM + " m is non-physical "
					+ "(must be positive and no more than " + MAX_RECTANGULAR_DIMENSION_M + " m) -- check inputs");
		}
		return freeDimensionM;
	}

	/**
	 * ASHRAE's equivalent-diameter formula, {@code De = 1.30*(a*b)^0.625/(a+b)^0.25}. Unlike the
	 * friction-factor/Reynolds-number formulas this package's Javadoc warns about, this one is
	 * dimensionally self-consistent: its exponents on the two length-dimensioned terms
	 * (1.25 from {@code (a*b)^0.625} and 0.25 from {@code (a+b)^0.25}) already sum to a pure
	 * length-to-length ratio of degree 1, so {@code 1.30} is a true dimensionless constant, not a
	 * unit-specific one -- the formula produces the same physical answer whether {@code a}/
	 * {@code b} are in metres or millimetres (verified numerically: a=0.5m,b=0.3m gives
	 * De=0.42015m; a=500mm,b=300mm gives De=420.35mm, the same length to rounding). Operating
	 * directly in metres here avoids a pointless mm round-trip.
	 */
	private static double equivalentDiameterM(double widthM, double heightM) {
		return 1.30 * Math.pow(widthM * heightM, 0.625) / Math.pow(widthM + heightM, 0.25);
	}

	private static FrictionRateAtDiameter frictionRateAt(
			double diameterM, double flowRateM3s, FluidProperties air, double roughnessM, FrictionFactorMethod method) {
		double area = (Math.PI / 4.0) * diameterM * diameterM;
		double velocityMs = flowRateM3s / area;
		double reynoldsNumber = (air.densityKgM3() * velocityMs * diameterM) / air.dynamicViscosityPaS();
		double relativeRoughness = roughnessM / diameterM;

		double frictionFactor = reynoldsNumber < LAMINAR_UPPER_RE
				? 64.0 / reynoldsNumber
				: FrictionFactorCalculator.frictionFactor(reynoldsNumber, relativeRoughness, method);

		double frictionRatePaPerM = frictionFactor * (1.0 / diameterM) * (air.densityKgM3() * velocityMs * velocityMs / 2.0);

		return new FrictionRateAtDiameter(velocityMs, reynoldsNumber, frictionFactor, frictionRatePaPerM);
	}

	private record FrictionRateAtDiameter(double velocityMs, double reynoldsNumber, double frictionFactor, double frictionRatePaPerM) {

	}

}
