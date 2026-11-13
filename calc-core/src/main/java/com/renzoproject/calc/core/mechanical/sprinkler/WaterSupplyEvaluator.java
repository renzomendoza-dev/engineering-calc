package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.mechanical.HazenWilliams;
import com.renzoproject.calc.core.mechanical.pipe.PipeDimensionResolver;
import com.renzoproject.calc.core.mechanical.sprinkler.SegmentRowBuilder.FittingLengths;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.DoubleUnaryOperator;

/**
 * The water supply of a sprinkler calculation reduced to the pressure it gives at a flow: a flow
 * test's curve as measured, or a gravity tank's head less the friction in its outlet piping. Part of
 * {@link SprinklerHydraulicCalculator}.
 */
final class WaterSupplyEvaluator {

	private final PipeDimensionResolver dimensionResolver;
	private final SegmentRowBuilder segmentRows;

	WaterSupplyEvaluator(PipeDimensionResolver dimensionResolver, SegmentRowBuilder segmentRows) {
		this.dimensionResolver = dimensionResolver;
		this.segmentRows = segmentRows;
	}

	/**
	 * Turns a supply spec into something that can state a pressure at a flow.
	 *
	 * <p>A flow test already knows its own curve. A gravity tank's has to be built here, because it
	 * depends on pipe bores and fitting equivalent lengths: full head at no flow, less the friction
	 * in the run down to the connection as flow rises.
	 */
	EvaluatedSupply evaluateSupply(WaterSupply supply, Set<String> warnings) {
		return switch (supply) {
			case SupplyCurve curve -> new EvaluatedSupply(curve::availablePressurePsi, curve.flowAtResidualGpm());
			case GravityTankSupply tank -> evaluateGravityTank(tank, warnings);
		};
	}

	private EvaluatedSupply evaluateGravityTank(GravityTankSupply tank, Set<String> warnings) {
		double staticPsi = tank.staticPressurePsi();
		List<ResolvedRun> runs = tank.outletRuns().stream().map(run -> {
			double insideDiameterInch = dimensionResolver
					.resolve(run.pipeMaterial(), run.schedule(), run.nominalSize())
					.internalDiameter()
					.to(SprinklerUnits.INCH)
					.getValue()
					.doubleValue();
			FittingLengths fittings = segmentRows.fittingEquivalentLengthFt(
					run.nominalSize(), run.cFactor(), run.fittings(), warnings);
			return new ResolvedRun(insideDiameterInch, run.cFactor(), run.pipeLengthFt() + fittings.totalFt());
		}).toList();

		if (runs.isEmpty()) {
			warnings.add("The gravity tank was given no outlet piping, so its full head is credited at every flow "
					+ "-- the friction between tank and connection is real, so this reads better than the system "
					+ "will perform");
		}

		DoubleUnaryOperator pressureAtFlow = flowGpm -> {
			double loss = runs.stream()
					.mapToDouble(run -> HazenWilliams.frictionLossPsiPerFoot(flowGpm, run.cFactor(), run.insideDiameterInch())
							* run.totalLengthFt())
					.sum();
			// A tank that has run out of head at this flow delivers nothing, not a negative push.
			return Math.max(0, staticPsi - loss);
		};
		return new EvaluatedSupply(pressureAtFlow, 0);
	}

	/** A supply reduced to "what pressure at this flow", plus a flow to scale a plot by. */
	record EvaluatedSupply(DoubleUnaryOperator pressureAtFlow, double nominalFlowGpm) {
	}

	/** One gravity tank outlet run with its reference data already resolved. */
	private record ResolvedRun(double insideDiameterInch, double cFactor, double totalLengthFt) {
	}

	/**
	 * Samples the supply at quarter points of its own flow scale plus the system's own demand, so a
	 * caller can plot demand against supply without re-deriving the curve.
	 *
	 * <p>A flow test sets that scale from the flow it was measured at. A gravity tank has no test
	 * flow to scale by, so the plot spans twice the demand instead -- enough to show the curve
	 * falling away on both sides of the point that matters.
	 */
	static List<SupplyCurvePoint> sampleSupplyCurve(EvaluatedSupply supply, double demandGpm) {
		double scale = supply.nominalFlowGpm() > 0 ? supply.nominalFlowGpm() : demandGpm * 2;
		Set<Double> flows = new TreeSet<>();
		for (double fraction : new double[] {0, 0.25, 0.5, 0.75, 1.0}) {
			flows.add(scale * fraction);
		}
		flows.add(demandGpm);
		return flows.stream()
				.map(flow -> new SupplyCurvePoint(flow, supply.pressureAtFlow().applyAsDouble(flow)))
				.toList();
	}

}
