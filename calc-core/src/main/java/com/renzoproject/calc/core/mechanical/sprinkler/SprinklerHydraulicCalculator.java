package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.firepump.FirePumpUnits;
import com.renzoproject.calc.core.mechanical.pipe.PipeDimensionResolver;
import com.renzoproject.calc.core.mechanical.pipe.PipeSizingMode;
import com.renzoproject.calc.core.mechanical.pipe.PipeSizingResult;
import com.renzoproject.calc.core.mechanical.pipe.PipeVelocityCalculator;
import com.renzoproject.calc.core.mechanical.pipe.PipeVelocityInput;
import com.renzoproject.calc.core.mechanical.pipe.RawDiameter;
import com.renzoproject.calc.core.mechanical.pipe.VelocityResult;
import tech.units.indriya.quantity.Quantities;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.DoubleUnaryOperator;

/**
 * NFPA 13 hydraulic calculation for a sprinkler system's remote design area, worked from the most
 * remote sprinkler back to the water supply.
 *
 * <p><b>Tree networks only.</b> The segment list defines the network, each segment pointing from
 * its upstream node toward the supply. Exactly one node may have no outgoing segment (the supply
 * point) and every node at most one outgoing segment. A loop, a second supply point or a dangling
 * node throws {@link CalculationException} -- gridded and looped systems need a simultaneous
 * network solution (Hardy Cross or similar), which this calculator deliberately does not attempt
 * rather than silently returning a wrong answer.
 *
 * <p><b>Junction balancing.</b> Where two branches meet, the branch arriving at the higher
 * pressure governs: the junction takes that pressure, and the lower-pressure branch must be pushed
 * harder to deliver against it. Since a branch behaves like a fixed-K path
 * ({@code Q is proportional to sqrt(P)}), its flow is scaled by
 * {@code sqrt(P_governing / P_branch)} and its segment friction losses are recomputed at the scaled
 * flow. This is the standard hand-calculation method, and it is an approximation: each rescaled
 * segment keeps its original upstream pressure while its own loss is recomputed, so the rescaled
 * branch's arrival pressure is not re-derived to exactly equal the governing pressure. The
 * junction uses the governing pressure regardless. A fully consistent answer would need iteration;
 * NFPA 13 practice does not require it.
 *
 * <p><b>Units:</b> psi, US gpm, feet, inches, fps throughout -- see {@code package-info.java}.
 * Friction loss is Hazen-Williams ({@link HazenWilliams}), as NFPA 13 requires, not the
 * Darcy-Weisbach used by {@code mechanical.pipe.PipePressureLossCalculator}.
 */
public class SprinklerHydraulicCalculator implements Calculator<SprinklerHydraulicInput, SprinklerHydraulicResult> {

	/** NFPA 13 does not cap velocity, but above this most reviewers expect justification. */
	private static final double VELOCITY_WARNING_FPS = 20.0;

	/** Below this margin the design is technically passing but has little room for error. */
	private static final double MARGIN_WARNING_PERCENT = 10.0;

	private static final double PRESSURE_EPSILON_PSI = 1e-9;

	private static final String VERIFIED_CONFIDENCE = "verified";

	private final PipeDimensionResolver dimensionResolver;
	private final FittingEquivalentLengthResolver fittingResolver;
	private final PipeVelocityCalculator velocityCalculator;
	private final RemoteAreaRulesResolver remoteAreaRulesResolver;

	/**
	 * @param dimensionResolver resolves each segment's actual inside diameter from
	 *                          {@code reference/pipes/*.json} -- the nominal size is never used as
	 *                          a bore
	 * @param fittingResolver   resolves fitting equivalent lengths
	 * @param velocityCalculator reused for the per-segment velocity readout rather than repeating
	 *                          {@code V = Q/A} here
	 */
	public SprinklerHydraulicCalculator(
			PipeDimensionResolver dimensionResolver,
			FittingEquivalentLengthResolver fittingResolver,
			PipeVelocityCalculator velocityCalculator) {
		this(dimensionResolver, fittingResolver, velocityCalculator, new JsonRemoteAreaRulesResolver());
	}

	/**
	 * @param remoteAreaRulesResolver supplies the minimum operating pressure the first sprinkler is
	 *                                floored at, so the hydraulic calculation and the remote area plan
	 *                                use the one figure
	 */
	public SprinklerHydraulicCalculator(
			PipeDimensionResolver dimensionResolver,
			FittingEquivalentLengthResolver fittingResolver,
			PipeVelocityCalculator velocityCalculator,
			RemoteAreaRulesResolver remoteAreaRulesResolver) {
		this.remoteAreaRulesResolver = remoteAreaRulesResolver;
		this.dimensionResolver = dimensionResolver;
		this.fittingResolver = fittingResolver;
		this.velocityCalculator = velocityCalculator;
	}

	@Override
	public SprinklerHydraulicResult calculate(SprinklerHydraulicInput input) {
		Network network = Network.build(input);
		Set<String> warnings = new LinkedHashSet<>();

		SolvedNode supply = solve(network.supplyNodeId(), network, input, warnings);

		double deviceLossPsi = input.deviceLosses().stream().mapToDouble(DeviceLoss::pressureLossPsi).sum();
		double requiredPsi = supply.pressurePsi() + deviceLossPsi;
		double demandGpm = supply.flowGpm();

		for (SegmentFlowRow row : supply.rows()) {
			if (row.velocityFps() > VELOCITY_WARNING_FPS) {
				warnings.add("Velocity in segment " + row.fromNodeId() + "->" + row.toNodeId() + " is "
						+ round(row.velocityFps()) + " fps, above the " + VELOCITY_WARNING_FPS
						+ " fps that normally needs justification");
			}
		}

		// No supply: the demand half of the answer is complete and correct on its own, and the
		// comparison half is left unanswered rather than filled in against a supply nobody gave.
		if (input.waterSupply() == null) {
			warnings.add("No water supply given -- the demand figures below are complete, but nothing has been "
					+ "compared against them, so there is no safety margin or pass/fail verdict");
			return new SprinklerHydraulicResult(
					List.copyOf(supply.rows()), demandGpm, requiredPsi, null, null, null, null, List.of(),
					List.copyOf(warnings));
		}

		EvaluatedSupply evaluated = evaluateSupply(input.waterSupply(), warnings);
		double availablePsi = evaluated.pressureAtFlow().applyAsDouble(demandGpm);
		double marginPsi = availablePsi - requiredPsi;
		double marginPercent = availablePsi == 0 ? 0 : (marginPsi / availablePsi) * 100.0;
		boolean passes = marginPsi >= 0;

		if (passes && marginPercent < MARGIN_WARNING_PERCENT) {
			warnings.add("Safety margin is " + round(marginPercent) + "%, below " + MARGIN_WARNING_PERCENT
					+ "% -- little room for supply deterioration or as-built deviation");
		}

		return new SprinklerHydraulicResult(
				List.copyOf(supply.rows()),
				demandGpm,
				requiredPsi,
				availablePsi,
				marginPsi,
				marginPercent,
				passes,
				sampleSupplyCurve(evaluated, demandGpm),
				List.copyOf(warnings));
	}

	/**
	 * Turns a supply spec into something that can state a pressure at a flow.
	 *
	 * <p>A flow test already knows its own curve. A gravity tank's has to be built here, because it
	 * depends on pipe bores and fitting equivalent lengths that only this class holds resolvers
	 * for: full head at no flow, less the friction in the run down to the connection as flow rises.
	 */
	private EvaluatedSupply evaluateSupply(WaterSupply supply, Set<String> warnings) {
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
			FittingLengths fittings = fittingEquivalentLengthFt(
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
	private record EvaluatedSupply(DoubleUnaryOperator pressureAtFlow, double nominalFlowGpm) {
	}

	/** One gravity tank outlet run with its reference data already resolved. */
	private record ResolvedRun(double insideDiameterInch, double cFactor, double totalLengthFt) {
	}

	/**
	 * Solves the subtree feeding {@code nodeId} and returns the pressure at that node, the flow
	 * arriving there, and the calculation rows for everything upstream of it (most remote first).
	 */
	private SolvedNode solve(String nodeId, Network network, SprinklerHydraulicInput input, Set<String> warnings) {
		List<SprinklerPipeSegment> incoming = network.incoming(nodeId);
		Optional<RemoteAreaSprinkler> sprinkler = network.sprinklerAt(nodeId);
		double hoseGpm = Objects.equals(nodeId, input.hoseStreamNodeId()) ? input.hoseStreamGpm() : 0.0;

		if (incoming.isEmpty()) {
			RemoteAreaSprinkler remote = sprinkler.orElseThrow(() -> new CalculationException(
					"Node " + nodeId + " has nothing flowing into it and no sprinkler -- every end of the network "
							+ "must be a flowing sprinkler"));
			double startPressurePsi = startingPressurePsi(remote, input);
			double flowGpm = SprinklerMath.flowGpm(remote.kFactor(), startPressurePsi);
			return new SolvedNode(startPressurePsi, flowGpm + hoseGpm, List.of());
		}

		List<Branch> branches = new ArrayList<>();
		for (SprinklerPipeSegment segment : incoming) {
			SolvedNode upstream = solve(segment.fromNodeId(), network, input, warnings);
			SegmentFlowRow row = computeRow(segment, upstream.flowGpm(), upstream.pressurePsi(), warnings);
			List<SegmentFlowRow> rows = new ArrayList<>(upstream.rows());
			rows.add(row);
			branches.add(new Branch(upstream.flowGpm(), row.endPressurePsi(), rows));
		}

		double governingPsi = branches.stream().mapToDouble(Branch::arrivalPressurePsi).max().orElseThrow();
		if (sprinkler.isPresent()) {
			governingPsi = Math.max(governingPsi, minimumFlowPressurePsi(sprinkler.get(), input));
		}

		List<SegmentFlowRow> rows = new ArrayList<>();
		double totalFlowGpm = hoseGpm;
		for (Branch branch : branches) {
			if (branch.arrivalPressurePsi() < governingPsi - PRESSURE_EPSILON_PSI) {
				if (branch.arrivalPressurePsi() <= 0) {
					throw new CalculationException("Cannot balance the branch arriving at node " + nodeId
							+ ": it arrives at " + round(branch.arrivalPressurePsi()) + " psi, so there is no "
							+ "pressure to scale its flow against -- check the elevation changes on that branch");
				}
				double factor = Math.sqrt(governingPsi / branch.arrivalPressurePsi());
				rows.addAll(rescale(branch.rows(), factor));
				totalFlowGpm += branch.flowGpm() * factor;
			} else {
				rows.addAll(branch.rows());
				totalFlowGpm += branch.flowGpm();
			}
		}
		if (sprinkler.isPresent()) {
			totalFlowGpm += SprinklerMath.flowGpm(sprinkler.get().kFactor(), governingPsi);
		}

		return new SolvedNode(governingPsi, totalFlowGpm, rows);
	}

	/**
	 * The starting pressure at the most remote sprinkler: the greater of the pressure its minimum
	 * required flow ({@code density * coverage area}) needs, and the caller's assumed starting
	 * pressure.
	 */
	/**
	 * The pressure the most remote sprinkler starts at: the greatest of what its share of the
	 * design density needs, the caller's assumed figure, and the minimum operating pressure.
	 *
	 * <p>The last of those is the floor. {@code assumedStartPressurePsi} used to be the only thing
	 * keeping this at 7 psi, which a caller could lower (to 0, to "let the minimum flow govern") and
	 * so start a calculation below any pressure a sprinkler is taken to operate at. The caller's
	 * figure can still raise the start pressure; it can no longer take it below the floor.
	 */
	private double startingPressurePsi(RemoteAreaSprinkler sprinkler, SprinklerHydraulicInput input) {
		double floor = remoteAreaRulesResolver.rules().minimumOperatingPressurePsi();
		return Math.max(
				floor, Math.max(minimumFlowPressurePsi(sprinkler, input), input.assumedStartPressurePsi()));
	}

	private static double minimumFlowPressurePsi(RemoteAreaSprinkler sprinkler, SprinklerHydraulicInput input) {
		double minimumFlowGpm = input.densityGpmPerSqFt() * sprinkler.coverageAreaSqFt();
		return SprinklerMath.pressurePsi(sprinkler.kFactor(), minimumFlowGpm);
	}

	private SegmentFlowRow computeRow(
			SprinklerPipeSegment segment, double flowGpm, double upstreamPressurePsi, Set<String> warnings) {
		double insideDiameterInch = dimensionResolver
				.resolve(segment.pipeMaterial(), segment.schedule(), segment.nominalSize())
				.internalDiameter()
				.to(SprinklerUnits.INCH)
				.getValue()
				.doubleValue();
		FittingLengths fittingLengths = fittingEquivalentLengthFt(segment, warnings);
		return buildRow(segment, flowGpm, upstreamPressurePsi, insideDiameterInch, fittingLengths);
	}

	private SegmentFlowRow buildRow(
			SprinklerPipeSegment segment,
			double flowGpm,
			double upstreamPressurePsi,
			double insideDiameterInch,
			FittingLengths fittingLengths) {
		double fittingLengthFt = fittingLengths.totalFt();
		double totalLengthFt = segment.pipeLengthFt() + fittingLengthFt;
		double psiPerFoot = HazenWilliams.frictionLossPsiPerFoot(flowGpm, segment.cFactor(), insideDiameterInch);
		double frictionPsi = psiPerFoot * totalLengthFt;
		double elevationPsi = ElevationPressure.psiForRise(segment.elevationChangeFt());
		double endPressurePsi = upstreamPressurePsi + frictionPsi + elevationPsi;

		return new SegmentFlowRow(
				segment.fromNodeId(),
				segment.toNodeId(),
				flowGpm,
				segment.nominalSize(),
				insideDiameterInch,
				segment.cFactor(),
				segment.pipeLengthFt(),
				fittingLengthFt,
				fittingLengths.cFactorAdjustment(),
				totalLengthFt,
				psiPerFoot,
				frictionPsi,
				elevationPsi,
				endPressurePsi,
				velocityFps(flowGpm, insideDiameterInch));
	}

	/**
	 * Rescales a branch's rows to a higher flow after junction balancing. Each row keeps its
	 * original upstream pressure and has its own friction loss recomputed at the scaled flow --
	 * see this class's Javadoc for why that is the standard approximation rather than a full
	 * re-solve.
	 */
	private List<SegmentFlowRow> rescale(List<SegmentFlowRow> rows, double factor) {
		List<SegmentFlowRow> rescaled = new ArrayList<>(rows.size());
		for (SegmentFlowRow row : rows) {
			double upstreamPressurePsi = row.endPressurePsi() - row.frictionLossPsi() - row.elevationPressurePsi();
			double scaledFlowGpm = row.flowGpm() * factor;
			double psiPerFoot = HazenWilliams.frictionLossPsiPerFoot(scaledFlowGpm, row.cFactor(), row.insideDiameterInch());
			double frictionPsi = psiPerFoot * row.totalLengthFt();

			rescaled.add(new SegmentFlowRow(
					row.fromNodeId(),
					row.toNodeId(),
					scaledFlowGpm,
					row.nominalSize(),
					row.insideDiameterInch(),
					row.cFactor(),
					row.pipeLengthFt(),
					row.fittingEquivalentLengthFt(),
					row.fittingCFactorAdjustment(),
					row.totalLengthFt(),
					psiPerFoot,
					frictionPsi,
					row.elevationPressurePsi(),
					upstreamPressurePsi + frictionPsi + row.elevationPressurePsi(),
					velocityFps(scaledFlowGpm, row.insideDiameterInch())));
		}
		return rescaled;
	}

	/**
	 * Sums a segment's fittings. Lengths looked up from the published chart are rescaled from its
	 * C=120 basis to the segment's own C, as NFPA 13 requires; an explicitly supplied length is
	 * used exactly as given, because the caller's basis is unknown and silently rescaling someone
	 * else's number would be worse than leaving it alone.
	 */
	private FittingLengths fittingEquivalentLengthFt(SprinklerPipeSegment segment, Set<String> warnings) {
		return fittingEquivalentLengthFt(segment.nominalSize(), segment.cFactor(), segment.fittings(), warnings);
	}

	/**
	 * Takes the three things that actually matter rather than a segment, so a gravity tank's outlet
	 * runs resolve their fittings through exactly this code -- including the C factor rescaling and
	 * the unverified-data warning -- instead of a parallel copy that could drift from it.
	 */
	private FittingLengths fittingEquivalentLengthFt(
			String nominalSize, double cFactor, List<SegmentFitting> fittings, Set<String> warnings) {
		double publishedFtTotal = 0;
		double explicitFtTotal = 0;

		for (SegmentFitting fitting : fittings) {
			if (fitting.explicitEquivalentLengthFt() != null) {
				explicitFtTotal += fitting.quantity() * fitting.explicitEquivalentLengthFt();
				continue;
			}

			FittingEquivalentLengthRow row = fittingResolver.resolve(fitting.fittingType());
			Double publishedFt = row.equivalentLengthFtByNominalSize().get(nominalSize);
			if (publishedFt == null) {
				throw new CalculationException("No published equivalent length for fitting " + fitting.fittingType()
						+ " at nominal size " + nominalSize + " (reference data confidence: "
						+ row.confidence() + ") -- supply NFPA 13 Table 27.2.3.1.1 in "
						+ "reference/sprinkler/fitting-equivalent-lengths.json, or pass an "
						+ "explicitEquivalentLengthFt on the fitting");
			}
			if (!VERIFIED_CONFIDENCE.equalsIgnoreCase(row.confidence())) {
				warnings.add("Fitting equivalent length for " + row.fittingType() + " is reference data tagged '"
						+ row.confidence() + "' -- confirm against NFPA 13 Table 27.2.3.1.1 before relying on this "
						+ "calculation");
			}
			publishedFtTotal += fitting.quantity() * publishedFt;
		}

		if (publishedFtTotal == 0) {
			return new FittingLengths(explicitFtTotal, 1.0);
		}

		double adjustment = HazenWilliams.equivalentLengthAdjustmentFactor(
				cFactor, fittingResolver.cFactorBasis());
		return new FittingLengths(publishedFtTotal * adjustment + explicitFtTotal, adjustment);
	}

	/** Reuses {@link PipeVelocityCalculator} rather than repeating {@code V = Q/A}. */
	private double velocityFps(double flowGpm, double insideDiameterInch) {
		if (flowGpm <= 0) {
			return 0;
		}
		PipeVelocityInput velocityInput = new PipeVelocityInput(
				PipeSizingMode.VELOCITY_FROM_DIAMETER,
				Quantities.getQuantity(flowGpm, FirePumpUnits.GPM),
				new RawDiameter(Quantities.getQuantity(insideDiameterInch, SprinklerUnits.INCH)),
				null,
				null,
				null);
		PipeSizingResult result = velocityCalculator.calculate(velocityInput);
		if (result instanceof VelocityResult velocity) {
			return velocity.velocity().to(SprinklerUnits.FOOT_PER_SECOND).getValue().doubleValue();
		}
		throw new CalculationException("PipeVelocityCalculator returned " + result.getClass().getSimpleName()
				+ " for a VELOCITY_FROM_DIAMETER request, which should never happen");
	}

	/**
	 * Samples the supply at quarter points of its own flow scale plus the system's own demand, so a
	 * caller can plot demand against supply without re-deriving the curve.
	 *
	 * <p>A flow test sets that scale from the flow it was measured at. A gravity tank has no test
	 * flow to scale by, so the plot spans twice the demand instead -- enough to show the curve
	 * falling away on both sides of the point that matters.
	 */
	private static List<SupplyCurvePoint> sampleSupplyCurve(EvaluatedSupply supply, double demandGpm) {
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

	private static double round(double value) {
		return Math.round(value * 100.0) / 100.0;
	}

	/**
	 * A segment's total fitting equivalent length and the C factor adjustment that produced it
	 * ({@code 1.0} when nothing was looked up from the published chart).
	 */
	private record FittingLengths(double totalFt, double cFactorAdjustment) {
	}

	/** Pressure and flow at a node, with the calculation rows for everything upstream of it. */
	private record SolvedNode(double pressurePsi, double flowGpm, List<SegmentFlowRow> rows) {
	}

	/** One already-solved branch arriving at a junction, before balancing. */
	private record Branch(double flowGpm, double arrivalPressurePsi, List<SegmentFlowRow> rows) {
	}

	/**
	 * The validated network shape: who feeds whom, where the supply is, and which nodes have
	 * sprinklers. Built once per calculation so the recursive solve can stay simple.
	 */
	private record Network(
			String supplyNodeId,
			Map<String, List<SprinklerPipeSegment>> incomingByNode,
			Map<String, RemoteAreaSprinkler> sprinklersByNode) {

		static Network build(SprinklerHydraulicInput input) {
			Map<String, SprinklerPipeSegment> outgoingByNode = new LinkedHashMap<>();
			Map<String, List<SprinklerPipeSegment>> incomingByNode = new LinkedHashMap<>();
			Set<String> nodes = new LinkedHashSet<>();

			for (SprinklerPipeSegment segment : input.segments()) {
				SprinklerPipeSegment existing = outgoingByNode.putIfAbsent(segment.fromNodeId(), segment);
				if (existing != null) {
					throw new CalculationException("Node " + segment.fromNodeId() + " has more than one outgoing "
							+ "segment (to " + existing.toNodeId() + " and to " + segment.toNodeId() + ") -- this "
							+ "calculator solves tree networks only, where flow from a node has exactly one path "
							+ "toward the supply");
				}
				incomingByNode.computeIfAbsent(segment.toNodeId(), key -> new ArrayList<>()).add(segment);
				nodes.add(segment.fromNodeId());
				nodes.add(segment.toNodeId());
			}

			List<String> terminals = nodes.stream().filter(node -> !outgoingByNode.containsKey(node)).toList();
			if (terminals.size() != 1) {
				throw new CalculationException("The network must have exactly one supply point (a node with no "
						+ "outgoing segment), but found " + terminals.size() + ": " + terminals
						+ " -- check the segment directions, which must all point toward the supply");
			}
			String supplyNodeId = terminals.get(0);

			for (String node : nodes) {
				Set<String> visited = new LinkedHashSet<>();
				String current = node;
				while (current != null && !current.equals(supplyNodeId)) {
					if (!visited.add(current)) {
						throw new CalculationException("The network contains a loop through node " + current
								+ " -- this calculator solves tree networks only; a looped or gridded system needs "
								+ "a simultaneous network solution it does not implement");
					}
					SprinklerPipeSegment next = outgoingByNode.get(current);
					current = next == null ? null : next.toNodeId();
				}
			}

			Map<String, RemoteAreaSprinkler> sprinklersByNode = new LinkedHashMap<>();
			for (RemoteAreaSprinkler sprinkler : input.sprinklers()) {
				if (!nodes.contains(sprinkler.nodeId())) {
					throw new CalculationException("Sprinkler node " + sprinkler.nodeId() + " is not an end of any "
							+ "pipe segment -- every sprinkler must be connected to the network");
				}
				sprinklersByNode.put(sprinkler.nodeId(), sprinkler);
			}

			if (input.hoseStreamGpm() > 0 && !nodes.contains(input.hoseStreamNodeId())) {
				throw new CalculationException("Hose stream node " + input.hoseStreamNodeId() + " is not an end of "
						+ "any pipe segment");
			}

			return new Network(supplyNodeId, incomingByNode, sprinklersByNode);
		}

		List<SprinklerPipeSegment> incoming(String nodeId) {
			return incomingByNode.getOrDefault(nodeId, List.of());
		}

		Optional<RemoteAreaSprinkler> sprinklerAt(String nodeId) {
			return Optional.ofNullable(sprinklersByNode.get(nodeId));
		}

	}

}
