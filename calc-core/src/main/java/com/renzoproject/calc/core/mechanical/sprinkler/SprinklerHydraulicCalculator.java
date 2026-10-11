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
		double availablePsi = input.waterSupply().availablePressurePsi(demandGpm);
		double marginPsi = availablePsi - requiredPsi;
		double marginPercent = availablePsi == 0 ? 0 : (marginPsi / availablePsi) * 100.0;
		boolean passes = marginPsi >= 0;

		for (SegmentFlowRow row : supply.rows()) {
			if (row.velocityFps() > VELOCITY_WARNING_FPS) {
				warnings.add("Velocity in segment " + row.fromNodeId() + "->" + row.toNodeId() + " is "
						+ round(row.velocityFps()) + " fps, above the " + VELOCITY_WARNING_FPS
						+ " fps that normally needs justification");
			}
		}
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
				sampleSupplyCurve(input.waterSupply(), demandGpm),
				List.copyOf(warnings));
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
	private static double startingPressurePsi(RemoteAreaSprinkler sprinkler, SprinklerHydraulicInput input) {
		return Math.max(minimumFlowPressurePsi(sprinkler, input), input.assumedStartPressurePsi());
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
		double fittingLengthFt = fittingEquivalentLengthFt(segment, warnings);
		return buildRow(segment, flowGpm, upstreamPressurePsi, insideDiameterInch, fittingLengthFt);
	}

	private SegmentFlowRow buildRow(
			SprinklerPipeSegment segment,
			double flowGpm,
			double upstreamPressurePsi,
			double insideDiameterInch,
			double fittingLengthFt) {
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
					row.totalLengthFt(),
					psiPerFoot,
					frictionPsi,
					row.elevationPressurePsi(),
					upstreamPressurePsi + frictionPsi + row.elevationPressurePsi(),
					velocityFps(scaledFlowGpm, row.insideDiameterInch())));
		}
		return rescaled;
	}

	private double fittingEquivalentLengthFt(SprinklerPipeSegment segment, Set<String> warnings) {
		double total = 0;
		for (SegmentFitting fitting : segment.fittings()) {
			if (fitting.explicitEquivalentLengthFt() != null) {
				total += fitting.quantity() * fitting.explicitEquivalentLengthFt();
				continue;
			}

			FittingEquivalentLengthRow row = fittingResolver.resolve(fitting.fittingType());
			Double publishedFt = row.equivalentLengthFtByNominalSize().get(segment.nominalSize());
			if (publishedFt == null) {
				throw new CalculationException("No published equivalent length for fitting " + fitting.fittingType()
						+ " at nominal size " + segment.nominalSize() + " (reference data confidence: "
						+ row.confidence() + ") -- supply NFPA 13 Table 27.2.3.1.1 in "
						+ "reference/sprinkler/fitting-equivalent-lengths.json, or pass an "
						+ "explicitEquivalentLengthFt on the fitting");
			}
			if (!VERIFIED_CONFIDENCE.equalsIgnoreCase(row.confidence())) {
				warnings.add("Fitting equivalent length for " + row.fittingType() + " is reference data tagged '"
						+ row.confidence() + "' -- confirm against NFPA 13 Table 27.2.3.1.1 before relying on this "
						+ "calculation");
			}
			total += fitting.quantity() * publishedFt;
		}
		return total;
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
	 * Samples the supply curve at quarter points of the flow test plus the system's own demand, so
	 * a caller can plot demand against supply without re-deriving the curve.
	 */
	private static List<SupplyCurvePoint> sampleSupplyCurve(SupplyCurve curve, double demandGpm) {
		Set<Double> flows = new TreeSet<>();
		for (double fraction : new double[] {0, 0.25, 0.5, 0.75, 1.0}) {
			flows.add(curve.flowAtResidualGpm() * fraction);
		}
		flows.add(demandGpm);
		return flows.stream().map(flow -> new SupplyCurvePoint(flow, curve.availablePressurePsi(flow))).toList();
	}

	private static double round(double value) {
		return Math.round(value * 100.0) / 100.0;
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
