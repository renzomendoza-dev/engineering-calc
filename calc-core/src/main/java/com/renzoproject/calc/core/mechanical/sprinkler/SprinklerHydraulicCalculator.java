package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.HazenWilliams;
import com.renzoproject.calc.core.mechanical.pipe.PipeDimensionResolver;
import com.renzoproject.calc.core.mechanical.pipe.PipeVelocityCalculator;
import com.renzoproject.calc.core.mechanical.sprinkler.WaterSupplyEvaluator.EvaluatedSupply;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
 * <p><b>Identical branches.</b> A branch may be declared a copy of another ({@link IdenticalBranch})
 * instead of listed. It is solved as the reference branch's own result -- worked from its most
 * remote sprinkler, before balancing -- arriving at its own junction and balanced there like any
 * other branch: {@code Q = Q_reference * sqrt(P_junction / P_reference)}. Nothing about it is
 * approximated beyond what listing it sprinkler by sprinkler would be, because that is exactly
 * what it stands in for; it just isn't typed out.
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

	private final RemoteAreaRulesResolver remoteAreaRulesResolver;
	private final SegmentRowBuilder segmentRows;
	private final WaterSupplyEvaluator supplyEvaluator;

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
		this.segmentRows = new SegmentRowBuilder(dimensionResolver, fittingResolver, velocityCalculator);
		this.supplyEvaluator = new WaterSupplyEvaluator(dimensionResolver, segmentRows);
	}

	@Override
	public SprinklerHydraulicResult calculate(SprinklerHydraulicInput input) {
		SprinklerNetwork network = SprinklerNetwork.build(input);
		Set<String> warnings = new LinkedHashSet<>();

		Trace trace = new Trace(new ArrayList<>(), new LinkedHashSet<>());
		SolvedNode supply = solve(network.supplyNodeId(), network, input, warnings, trace);
		List<IdenticalBranchRow> identicalRows = List.copyOf(trace.identicalRows());

		double deviceLossPsi = input.deviceLosses().stream().mapToDouble(DeviceLoss::pressureLossPsi).sum();
		double requiredPsi = supply.pressurePsi() + deviceLossPsi;
		double demandGpm = supply.flowGpm();
		warnAboveMaximumPressure(supply.sprinklers(), input.hazard(), "", warnings);

		for (SegmentFlowRow row : supply.rows()) {
			if (row.velocityFps() > VELOCITY_WARNING_FPS) {
				warnings.add("Velocity in segment " + row.fromNodeId() + "->" + row.toNodeId() + " is "
						+ round(row.velocityFps()) + " fps (" + carrying(row) + "), above the "
						+ VELOCITY_WARNING_FPS + " fps that normally needs justification");
			}
		}

		// No supply: the demand half of the answer is complete and correct on its own, and the
		// comparison half is left unanswered rather than filled in against a supply nobody gave.
		if (input.waterSupply() == null) {
			// The web page recognises this note by its opening words ("No water supply given") to show it
			// on the results only; keep them if this is reworded.
			warnings.add("No water supply given -- the demand figures are complete, but with nothing to "
					+ "compare against there is no safety margin or pass/fail verdict");
			return new SprinklerHydraulicResult(
					List.copyOf(supply.rows()), demandGpm, requiredPsi, null, null, null, null, List.of(),
					List.copyOf(warnings), identicalRows, List.copyOf(supply.sprinklers()));
		}

		EvaluatedSupply evaluated = supplyEvaluator.evaluateSupply(input.waterSupply(), warnings);
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
				WaterSupplyEvaluator.sampleSupplyCurve(evaluated, demandGpm),
				List.copyOf(warnings),
				identicalRows,
				List.copyOf(supply.sprinklers()));
	}

	/**
	 * Solves the subtree feeding {@code nodeId} and returns the pressure at that node, the flow
	 * arriving there, and the calculation rows for everything upstream of it (most remote first).
	 */
	private SolvedNode solve(
			String nodeId, SprinklerNetwork network, SprinklerHydraulicInput input, Set<String> warnings, Trace trace) {
		List<SprinklerPipeSegment> incoming = network.incoming(nodeId);
		List<IdenticalBranch> copies = network.copiesJoiningAt(nodeId);
		Optional<RemoteAreaSprinkler> sprinkler = network.sprinklerAt(nodeId);
		double hoseGpm = Objects.equals(nodeId, input.hoseStreamNodeId()) ? input.hoseStreamGpm() : 0.0;

		if (incoming.isEmpty() && copies.isEmpty()) {
			RemoteAreaSprinkler remote = sprinkler.orElseThrow(() -> new CalculationException(
					"Node " + nodeId + " has nothing flowing into it and no sprinkler -- every end of the network "
							+ "must be a flowing sprinkler"));
			double startPressurePsi = startingPressurePsi(remote, input);
			double flowGpm = SprinklerMath.flowGpm(remote.kFactor(), startPressurePsi);
			return new SolvedNode(
					startPressurePsi,
					flowGpm + hoseGpm,
					List.of(),
					1,
					List.of(sprinklerRow(remote, flowGpm, startPressurePsi, input)));
		}

		List<Branch> branches = new ArrayList<>();
		for (SprinklerPipeSegment segment : incoming) {
			SolvedNode upstream = solve(segment.fromNodeId(), network, input, warnings, trace);
			SegmentFlowRow row = segmentRows.computeRow(segment, upstream.flowGpm(), upstream.pressurePsi(), warnings);
			List<SegmentFlowRow> rows = new ArrayList<>(upstream.rows());
			rows.add(row);
			branches.add(new Branch(
					upstream.flowGpm(),
					row.endPressurePsi(),
					rows,
					upstream.sprinklerCount(),
					upstream.sprinklers(),
					null,
					null));
		}
		for (IdenticalBranch copy : copies) {
			branches.add(copyOf(copy, network, input, warnings, trace));
		}

		double governingPsi = branches.stream().mapToDouble(Branch::arrivalPressurePsi).max().orElseThrow();
		if (sprinkler.isPresent()) {
			// Never below what the design density needs of it, nor below the minimum operating pressure.
			governingPsi = Math.max(
					governingPsi, Math.max(minimumFlowPressurePsi(sprinkler.get(), input), floorPsi(input)));
		}

		List<SegmentFlowRow> rows = new ArrayList<>();
		List<SprinklerFlowRow> sprinklerRows = new ArrayList<>();
		double totalFlowGpm = hoseGpm;
		int sprinklerCount = sprinkler.isPresent() ? 1 : 0;
		for (Branch branch : branches) {
			double factor = 1.0;
			if (branch.arrivalPressurePsi() < governingPsi - PRESSURE_EPSILON_PSI) {
				if (branch.arrivalPressurePsi() <= 0) {
					throw new CalculationException("Cannot balance the branch arriving at node " + nodeId
							+ ": it arrives at " + round(branch.arrivalPressurePsi()) + " psi, so there is no "
							+ "pressure to scale its flow against -- check the elevation changes on that branch");
				}
				factor = Math.sqrt(governingPsi / branch.arrivalPressurePsi());
			}
			List<SegmentFlowRow> balanced = factor == 1.0 ? branch.rows() : segmentRows.rescale(branch.rows(), factor);
			double flowGpm = branch.flowGpm() * factor;

			if (branch.copy() == null) {
				rows.addAll(balanced);
				sprinklerRows.addAll(factor == 1.0 ? branch.sprinklers() : scaleSprinklers(branch.sprinklers(), factor));
			} else {
				// A copy has no rows of its own -- its pipes were never listed. It is reported as the
				// one line that scaled it, and its velocities are checked here, at the scaled flow.
				for (SegmentFlowRow row : balanced) {
					if (row.velocityFps() > VELOCITY_WARNING_FPS) {
						warnings.add("Velocity in segment " + row.fromNodeId() + "->" + row.toNodeId()
								+ " of the branch identical to it that joins at " + nodeId + " is "
								+ round(row.velocityFps()) + " fps (" + carrying(row) + "), above the "
								+ VELOCITY_WARNING_FPS + " fps that normally needs justification");
					}
				}
				warnAboveMaximumPressure(
						factor == 1.0 ? branch.sprinklers() : scaleSprinklers(branch.sprinklers(), factor),
						input.hazard(),
						" in the branch identical to it that joins at " + nodeId,
						warnings);
				trace.identicalRows().add(new IdenticalBranchRow(
						nodeId,
						branch.copy().referenceNodeId(),
						branch.referenceJunctionNodeId(),
						branch.sprinklerCount(),
						branch.flowGpm(),
						branch.arrivalPressurePsi(),
						governingPsi,
						factor,
						flowGpm));
			}
			totalFlowGpm += flowGpm;
			sprinklerCount += branch.sprinklerCount();
		}
		if (sprinkler.isPresent()) {
			double ownFlowGpm = SprinklerMath.flowGpm(sprinkler.get().kFactor(), governingPsi);
			totalFlowGpm += ownFlowGpm;
			sprinklerRows.add(sprinklerRow(sprinkler.get(), ownFlowGpm, governingPsi, input));
		}

		return new SolvedNode(governingPsi, totalFlowGpm, rows, sprinklerCount, sprinklerRows);
	}

	/**
	 * The branch an {@link IdenticalBranch} stands for, as it would have been had it been listed: the
	 * reference node solved from its own most remote sprinkler, then the one pipe leaving it.
	 *
	 * <p>The reference is solved afresh rather than looked up. That keeps it independent of
	 * whichever order the tree happens to be walked in, and of any balancing the reference itself
	 * got at its own junction -- which is exactly what the copy must not inherit.
	 */
	private Branch copyOf(
			IdenticalBranch copy,
			SprinklerNetwork network,
			SprinklerHydraulicInput input,
			Set<String> warnings,
			Trace trace) {
		String reference = copy.referenceNodeId();
		if (!trace.activeReferences().add(reference)) {
			throw new CalculationException("Identical branches repeat each other in a circle through node "
					+ reference + " -- an identical branch has to repeat a branch that is actually listed");
		}
		try {
			SprinklerPipeSegment leaving = network.outgoing(reference);
			// A throwaway trace for the rows: whatever the reference holds is already reported (or
			// is internal to it) -- recording it again here would list it twice.
			SolvedNode own = solve(reference, network, input, warnings,
					new Trace(new ArrayList<>(), trace.activeReferences()));
			SegmentFlowRow row = segmentRows.computeRow(leaving, own.flowGpm(), own.pressurePsi(), warnings);
			List<SegmentFlowRow> rows = new ArrayList<>(own.rows());
			rows.add(row);
			return new Branch(own.flowGpm(), row.endPressurePsi(), rows, own.sprinklerCount(), own.sprinklers(),
					copy, leaving.toNodeId());
		} finally {
			trace.activeReferences().remove(reference);
		}
	}

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
		double floor = floorPsi(input);
		return Math.max(
				floor, Math.max(minimumFlowPressurePsi(sprinkler, input), input.assumedStartPressurePsi()));
	}

	/**
	 * The lowest pressure any sprinkler is taken to operate at: the standard's minimum (27.2.4.11.1),
	 * or the sprinkler listing's where that is higher (27.2.4.11.2). Applies to every sprinkler,
	 * not only the most remote.
	 */
	private double floorPsi(SprinklerHydraulicInput input) {
		return Math.max(
				remoteAreaRulesResolver.rules().minimumOperatingPressurePsi(), input.listedMinimumPressurePsi());
	}

	private static double minimumFlowPressurePsi(RemoteAreaSprinkler sprinkler, SprinklerHydraulicInput input) {
		double minimumFlowGpm = input.densityGpmPerSqFt() * sprinkler.coverageAreaSqFt();
		return SprinklerMath.pressurePsi(sprinkler.kFactor(), minimumFlowGpm);
	}

	private static SprinklerFlowRow sprinklerRow(
			RemoteAreaSprinkler sprinkler, double flowGpm, double pressurePsi, SprinklerHydraulicInput input) {
		return new SprinklerFlowRow(
				sprinkler.nodeId(),
				sprinkler.kFactor(),
				flowGpm,
				pressurePsi,
				input.densityGpmPerSqFt() * sprinkler.coverageAreaSqFt());
	}

	/**
	 * A balanced branch's sprinklers at the scaled flow. The whole branch is scaled by one factor
	 * (that is what makes it a fixed-K path), so each sprinkler's flow is scaled by it and its
	 * pressure is the one that flow corresponds to, {@code (Q / K)^2}.
	 */
	private static List<SprinklerFlowRow> scaleSprinklers(List<SprinklerFlowRow> rows, double factor) {
		List<SprinklerFlowRow> scaled = new ArrayList<>(rows.size());
		for (SprinklerFlowRow row : rows) {
			double flowGpm = row.flowGpm() * factor;
			scaled.add(new SprinklerFlowRow(
					row.nodeId(),
					row.kFactor(),
					flowGpm,
					SprinklerMath.pressurePsi(row.kFactor(), flowGpm),
					row.minimumFlowGpm()));
		}
		return scaled;
	}

	/**
	 * Warns for each sprinkler operating above the maximum operating pressure (NFPA 13 27.2.4.12).
	 *
	 * <p>The section states the cap for extra hazard occupancies and for the storage arrangements it
	 * lists; the form has no storage model. For extra hazard it is said as a limit, and for the other
	 * hazards as a figure to check the sprinkler's listing against, because claiming a limit this
	 * section does not state for them would be wrong in the other direction.
	 */
	private void warnAboveMaximumPressure(
			List<SprinklerFlowRow> rows, SprinklerHazard hazard, String where, Set<String> warnings) {
		RemoteAreaRules rules = remoteAreaRulesResolver.rules();
		double maximum = rules.maximumOperatingPressurePsi();
		if (maximum <= 0) {
			return;
		}
		boolean extraHazard = hazard == SprinklerHazard.EXTRA_GROUP_1 || hazard == SprinklerHazard.EXTRA_GROUP_2;
		for (SprinklerFlowRow row : rows) {
			if (row.pressurePsi() > maximum + PRESSURE_EPSILON_PSI) {
				warnings.add("Sprinkler " + row.nodeId() + where + " operates at " + plain(round(row.pressurePsi()))
						+ " psi, above the " + plain(maximum) + " psi "
						+ (extraHazard
								? "maximum operating pressure NFPA 13 (27.2.4.12) sets for extra hazard occupancies"
								: "NFPA 13 (27.2.4.12) sets as the maximum for extra hazard and storage occupancies "
										+ "-- confirm what this sprinkler's listing allows"));
			}
		}
	}

	/**
	 * What a pipe is carrying and in what size, for a velocity warning: the speed alone says a pipe
	 * is too small for its flow, but not which flow or which size, which is what the fix turns on.
	 */
	private static String carrying(SegmentFlowRow row) {
		return round(row.flowGpm()) + " gpm in " + row.nominalSize() + "\" pipe";
	}

	/** A figure without a trailing ".0": 175 reads as 175, 176.25 stays as it is. */
	private static String plain(double value) {
		return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
	}

	private static double round(double value) {
		return Math.round(value * 100.0) / 100.0;
	}

	/**
	 * Pressure and flow at a node, with the calculation rows for everything upstream of it and the
	 * number of flowing sprinklers that feed it.
	 */
	private record SolvedNode(
			double pressurePsi,
			double flowGpm,
			List<SegmentFlowRow> rows,
			int sprinklerCount,
			List<SprinklerFlowRow> sprinklers) {
	}

	/**
	 * One already-solved branch arriving at a junction, before balancing. {@code copy} is null for
	 * a branch that was listed, and for a copy it is the declaration, with
	 * {@code referenceJunctionNodeId} the node the reference's own final pipe lands on.
	 */
	private record Branch(
			double flowGpm,
			double arrivalPressurePsi,
			List<SegmentFlowRow> rows,
			int sprinklerCount,
			List<SprinklerFlowRow> sprinklers,
			IdenticalBranch copy,
			String referenceJunctionNodeId) {
	}

	/**
	 * What a solve accumulates beyond its return value: the line for each copy it balanced, and the
	 * references currently being worked, so copies that repeat each other are caught rather than
	 * recursing for ever.
	 */
	private record Trace(List<IdenticalBranchRow> identicalRows, Set<String> activeReferences) {
	}

}
