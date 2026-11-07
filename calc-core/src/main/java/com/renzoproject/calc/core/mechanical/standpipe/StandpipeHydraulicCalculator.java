package com.renzoproject.calc.core.mechanical.standpipe;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.PipeDimensionResolver;
import com.renzoproject.calc.core.mechanical.standpipe.StandpipeHydraulicResult.DemandLine;
import com.renzoproject.calc.core.mechanical.standpipe.StandpipeHydraulicResult.FdcLineRow;
import com.renzoproject.calc.core.mechanical.standpipe.StandpipeHydraulicResult.SegmentRow;
import java.util.ArrayList;
import java.util.List;

/**
 * Standpipe system hydraulics after NFPA 14: the flow the system must deliver for its class (10.6),
 * and the pressure needed at the supply to deliver it at the hydraulically most remote hose
 * connection, worked as a path from that connection back to the supply with Hazen-Williams friction
 * (11.3.3.1.1), fitting equivalent lengths (Table 11.3.1.3) and 0.433 psi per foot of elevation.
 *
 * <p>Clause numbers and values are those of the <b>2024</b> edition, read from its printed pages
 * (Chapters 5, 8, 10 and 11). The equivalent length table and the C values were first transcribed
 * from the 2019 edition and match the 2024 printing.
 *
 * <p>Deliberately separate from the sprinkler calculator: the demand rules, the residual-pressure
 * rule at a hose connection and the checks differ, and the sprinkler's tree network is not needed
 * for one path. {@code HazenWilliams}-style arithmetic is duplicated on purpose, as elsewhere.
 *
 * <p>Scope: one path (the most remote standpipe); flow of other standpipes and the sprinkler
 * allowance join at the supply as flow only, with no pressure check of their own.
 */
public class StandpipeHydraulicCalculator implements Calculator<StandpipeHydraulicInput, StandpipeHydraulicResult> {

	private static final double PSI_PER_FOOT = 0.433;
	private static final double VELOCITY_COEFFICIENT = 0.4085;
	private static final double MAXIMUM_PRESSURE_PSI = 400.0;
	/** 10.2.4.1.3 and 10.2.4.2: above this static pressure at a hose connection a listed pressure-regulating device is required. */
	private static final double STATIC_PRESSURE_LIMIT_PSI = 175.0;
	/** 10.6.1.2.1 and 10.2.6.1: the residual pressure at the connection point of each other standpipe, as at a 2.5 in. hose connection. */
	private static final double OTHER_STANDPIPE_RESIDUAL_PSI = 100.0;
	private static final double PRD_RESIDUAL_LIMIT_PSI = 100.0;
	private static final double MINIMUM_SUPPLY_MINUTES = 30.0;
	/**
	 * A.10.7: the conservative basis for what the fire department can deliver, a 1,000 gpm pumper at the NFPA 1901
	 * rating points: 1,000 gpm at 150 psi, 700 gpm at 200 psi and 500 gpm at 250 psi.
	 */
	private static final double PUMPER_RATED_FLOW_GPM = 1000.0;
	private static final double[][] PUMPER_RATING_POINTS_GPM_PSI = {{500.0, 250.0}, {700.0, 200.0}, {1000.0, 150.0}};
	/** A.10.7.3.1.3: the maximum working pressure of 4 in. and larger supply hose (NFPA 1961). */
	private static final double LARGE_HOSE_WORKING_PRESSURE_PSI = 185.0;

	private final PipeDimensionResolver dimensionResolver;
	private final StandpipeReferenceResolver referenceResolver;

	public StandpipeHydraulicCalculator(PipeDimensionResolver dimensionResolver, StandpipeReferenceResolver referenceResolver) {
		this.dimensionResolver = dimensionResolver;
		this.referenceResolver = referenceResolver;
	}

	@Override
	public StandpipeHydraulicResult calculate(StandpipeHydraulicInput input) {
		List<StandpipeCheck> checks = new ArrayList<>();
		Demand demand = demand(input, checks);

		List<SegmentRow> rows = new ArrayList<>();
		double flow = 0;
		double pressure = 0;
		double totalFriction = 0;
		double totalElevation = 0;
		double hoseConnectionFlow = 0;
		boolean first = true;

		for (StandpipeSegment segment : input.segments()) {
			flow += segment.flowAddedGpm();
			double remotePressure = pressure;
			boolean raised = false;
			if (segment.hoseConnectionSize() != null) {
				hoseConnectionFlow += segment.flowAddedGpm();
				double required = segment.hoseConnectionSize().minimumResidualPsi();
				if (remotePressure < required) {
					raised = !first;
					remotePressure = required;
				}
				connectionChecks(input, segment, remotePressure, checks);
			} else if (segment.otherStandpipeConnection()) {
				// Another standpipe's flow is added at its connection point to the common supply piping, at the minimum
				// residual (10.6.1.2.1), and not balanced to a higher pressure there (10.6.1.2.4).
				if (remotePressure < OTHER_STANDPIPE_RESIDUAL_PSI) {
					raised = !first;
					remotePressure = OTHER_STANDPIPE_RESIDUAL_PSI;
				}
				checks.add(new StandpipeCheck("10.6.1.2.1", segment.label() + ": another standpipe's connection point, residual pressure at least the minimum",
						CheckStatus.PASS, remotePressure, OTHER_STANDPIPE_RESIDUAL_PSI, "psi",
						"Taken as the minimum residual, or the pressure the path gives there if that is higher. Its " + Math.round(segment.flowAddedGpm())
								+ " gpm is not balanced to the higher pressure at the connection point (10.6.1.2.4)."));
			}
			first = false;
			if (flow <= 0) {
				throw new CalculationException("segment " + segment.label() + " carries no flow: a flow must enter at or before it");
			}

			double insideDiameter = insideDiameterInch(segment);
			double fittingsLength = fittingsLengthFt(segment, insideDiameter);
			double perFoot = 4.52 * Math.pow(flow, 1.85) / (Math.pow(segment.cFactor(), 1.85) * Math.pow(insideDiameter, 4.87));
			double friction = perFoot * (segment.pipeLengthFt() + fittingsLength);
			double elevation = PSI_PER_FOOT * segment.elevationRiseFt();
			pressure = remotePressure + friction + elevation;
			totalFriction += friction;
			totalElevation += elevation;

			rows.add(new SegmentRow(segment.label(), segment.role(), segment.hoseConnectionSize(), segment.nominalSize(), insideDiameter,
					segment.cFactor(), segment.flowAddedGpm(), flow, VELOCITY_COEFFICIENT * flow / (insideDiameter * insideDiameter),
					segment.pipeLengthFt(), fittingsLength, perFoot, friction, segment.elevationRiseFt(), elevation, remotePressure,
					pressure, raised, segment.otherStandpipeConnection()));
			sizeCheck(input, segment, checks);
		}

		// What joins at the supply: a figure the designer gave, or for Class I and III what the demand rules
		// ask for beyond the path (additional standpipes, sprinkler allowance). Class II has no combining
		// rule on the pages read, so nothing is assumed for it.
		boolean automatic = input.flowAddedAtSupplyGpm() == null && input.standpipeClass() != StandpipeClass.CLASS_II;
		double flowAdded = input.flowAddedAtSupplyGpm() != null
				? input.flowAddedAtSupplyGpm()
				: automatic ? Math.max(0.0, demand.totalGpm() - flow) : 0.0;
		double flowAtSupply = flow + flowAdded;
		pressure = otherStandpipesAtTheSupply(input, flowAdded, pressure, checks);
		supplyChecks(input, demand, hoseConnectionFlow, flowAtSupply, flowAdded, automatic, pressure, checks);
		InletSize inletSize = input.fireDepartmentConnectionInletSize();
		int requiredInlets = (int) Math.ceil(flowAtSupply / inletSize.flowPerInletGpm() - 1e-9);
		fdcCheck(input, inletSize, requiredInlets, checks);
		Double staticAtLowest = input.supplyStaticPressurePsi() == null ? null
				: input.supplyStaticPressurePsi() - PSI_PER_FOOT * (input.lowestHoseConnectionHeightFt() == null ? 0.0 : input.lowestHoseConnectionHeightFt());
		staticChecks(input, staticAtLowest, checks);
		List<FdcLineRow> fdcLines = fdcLines(input, inletSize, flowAtSupply, pressure, checks);
		Double highestFdcPressure = fdcLines.stream().map(FdcLineRow::pressureAtFdcPsi).max(Double::compare).orElse(null);
		if (input.manualSystem()) {
			manualChecks(highestFdcPressure != null ? highestFdcPressure : pressure, flowAtSupply, fdcLines.isEmpty(), checks);
			if (fdcLines.isEmpty()) {
				// With no connection piping given, the manual system's pressure at the supply is the one the fire department pumps in.
				pumperChecks("Fire department connection", pressure, flowAtSupply, inletSize, checks);
			}
		}

		return new StandpipeHydraulicResult(demand.lines(), demand.totalGpm(), demand.capped(), demand.mostRemoteGpm(), flowAtSupply, flowAdded,
				automatic, pressure,
				totalFriction, totalElevation, requiredInlets, inletSize, input.manualSystem(), input.supplyDurationMinutes(),
				flowAtSupply * input.supplyDurationMinutes(), fdcLines, highestFdcPressure, staticAtLowest, List.copyOf(rows), List.copyOf(checks));
	}

	/** A figure for a note: whole when it is whole, otherwise to a tenth. */
	private static String trim(double value) {
		return value == Math.rint(value) ? String.valueOf((long) value) : String.format(java.util.Locale.ROOT, "%.1f", value);
	}

	/**
	 * The static (nonflowing) pressure limits: 400 psi anywhere in the system at any time (10.2.1), and
	 * 175 psi at a hose connection, above which a listed pressure-regulating device is required
	 * (10.2.4.2 for 2.5 in., 10.2.4.1.3 for 1.5 in.). The highest static pressure at a hose connection
	 * is at the lowest one, the supply static pressure less the head of its height above the supply.
	 */
	private void staticChecks(StandpipeHydraulicInput input, Double staticAtLowest, List<StandpipeCheck> checks) {
		Double supplyStatic = input.supplyStaticPressurePsi();
		if (supplyStatic == null) {
			checks.add(new StandpipeCheck("10.2.4.2", "Static pressure at the lowest hose connection not above 175 psi", CheckStatus.OPEN, null,
					STATIC_PRESSURE_LIMIT_PSI, "psi",
					"Give the static pressure at the supply. For a fire pump it is the pump boost at churn plus the supply pressure at the "
							+ "suction (A.10.2). Above 175 psi static a listed pressure-regulating device is required at a 2.5 in. connection "
							+ "(10.2.4.2) and at a 1.5 in. connection (10.2.4.1.3)."));
			return;
		}
		checks.add(new StandpipeCheck("10.2.1", "Static pressure at the supply not above 400 psi", supplyStatic <= MAXIMUM_PRESSURE_PSI ? CheckStatus.PASS
				: CheckStatus.FAIL, supplyStatic, MAXIMUM_PRESSURE_PSI, "psi",
				"The maximum system pressure is normally at pump churn and includes both the pump boost and the supply pressure (A.10.2). "
						+ "Express mains to vertical zones may exceed 400 psi where their listings allow, with no hose valves on them (10.2.3)."));
		boolean above = staticAtLowest > STATIC_PRESSURE_LIMIT_PSI;
		double height = input.lowestHoseConnectionHeightFt() == null ? 0.0 : input.lowestHoseConnectionHeightFt();
		String basis = "Supply static " + Math.round(supplyStatic) + " psi, less 0.433 psi per foot over " + trim(height) + " ft.";
		checks.add(new StandpipeCheck("10.2.4.2", "Static pressure at the lowest hose connection not above 175 psi",
				above ? CheckStatus.CONFIRM : CheckStatus.PASS, staticAtLowest, STATIC_PRESSURE_LIMIT_PSI, "psi",
				above
						? basis + " A listed pressure-regulating device is required to limit the static and residual pressure at the hose "
								+ "connection to 175 psi, at a 2.5 in. connection (10.2.4.2) and at a 1.5 in. connection (10.2.4.1.3). The annex says "
								+ "that on a Class I standpipe one should not be required where the hose and nozzles of the responding fire "
								+ "company are rated for the pressures and the AHJ approves (A.10.2.4)."
						: basis));
	}

	/**
	 * The other standpipes of a Class I or III system join the common supply piping at their connection
	 * points, with their flow at the minimum residual pressure and not balanced to a higher one
	 * (10.6.1.2.1, 10.6.1.2.4, A.10.6.1.2.3). Those not placed on the path join at the supply end, so the
	 * pressure required there is at least that residual. Returns the pressure required at the supply end.
	 */
	private double otherStandpipesAtTheSupply(StandpipeHydraulicInput input, double flowAdded, double pressure, List<StandpipeCheck> checks) {
		boolean others = input.standpipeClass() != StandpipeClass.CLASS_II
				&& input.additionalStandpipes() + input.singleConnectionLaterals() > 0 && flowAdded > 0;
		if (!others) {
			return pressure;
		}
		boolean raised = pressure < OTHER_STANDPIPE_RESIDUAL_PSI;
		double required = Math.max(pressure, OTHER_STANDPIPE_RESIDUAL_PSI);
		checks.add(new StandpipeCheck("10.6.1.2.1", "Other standpipes joining at the supply end: residual pressure at their connection point at least the minimum",
				CheckStatus.PASS, required, OTHER_STANDPIPE_RESIDUAL_PSI, "psi",
				(raised ? "The path gives " + trim(pressure) + " psi there, so the pressure required at the supply is raised to the minimum residual. "
						: "The path gives more than the minimum there. ")
						+ "Their flow joins as flow only and is not balanced to the higher pressure (10.6.1.2.4). Place a standpipe's flow "
						+ "on the path, at its own connection point, where it joins the common supply piping elsewhere."));
		return required;
	}

	/**
	 * 10.4.1 and A.10.4.1: the pressure the fire department must deliver at each fire department
	 * connection for it to supply the system demand. Each connection's pipes are worked from the
	 * system end, where the pressure is that required at the supply end of the main path, out to the
	 * connection, with the flow the connection carries (the whole flow at the supply unless a share
	 * is given, as 10.7.3.2 allows for several connections).
	 */
	private List<FdcLineRow> fdcLines(StandpipeHydraulicInput input, InletSize inletSize, double flowAtSupply, double pressureAtSystem,
			List<StandpipeCheck> checks) {
		if (input.fireDepartmentConnections().isEmpty()) {
			if (input.standpipeClass() != StandpipeClass.CLASS_II) {
				checks.add(new StandpipeCheck("10.4.1", "Each fire department connection shown able to supply the system demand",
						CheckStatus.OPEN, null, flowAtSupply, "gpm",
						"Give the piping of each fire department connection to find the pressure the fire department must deliver at it (A.10.4.1)."));
			}
			return List.of();
		}

		List<FdcLineRow> lines = new ArrayList<>();
		boolean sharedFlow = false;
		double carried = 0;
		for (StandpipeFdcLine line : input.fireDepartmentConnections()) {
			double flow = line.flowGpm() != null ? line.flowGpm() : flowAtSupply;
			sharedFlow |= line.flowGpm() != null;
			carried += flow;

			List<SegmentRow> rows = new ArrayList<>();
			double pressure = pressureAtSystem;
			double friction = 0;
			double elevation = 0;
			for (StandpipeSegment segment : line.segments()) {
				double insideDiameter = insideDiameterInch(segment);
				double fittingsLength = fittingsLengthFt(segment, insideDiameter);
				double perFoot = 4.52 * Math.pow(flow, 1.85) / (Math.pow(segment.cFactor(), 1.85) * Math.pow(insideDiameter, 4.87));
				double segmentFriction = perFoot * (segment.pipeLengthFt() + fittingsLength);
				double segmentElevation = PSI_PER_FOOT * segment.elevationRiseFt();
				double systemEnd = pressure;
				pressure = systemEnd + segmentFriction + segmentElevation;
				friction += segmentFriction;
				elevation += segmentElevation;
				rows.add(new SegmentRow(segment.label(), segment.role(), null, segment.nominalSize(), insideDiameter, segment.cFactor(), 0.0,
						flow, VELOCITY_COEFFICIENT * flow / (insideDiameter * insideDiameter), segment.pipeLengthFt(), fittingsLength, perFoot,
						segmentFriction, segment.elevationRiseFt(), segmentElevation, systemEnd, pressure, false, false));
			}
			lines.add(new FdcLineRow(line.label(), flow, line.inlets(), List.copyOf(rows), friction, elevation, pressureAtSystem, pressure));

			checks.add(new StandpipeCheck("10.4.1", line.label() + ": pressure required at the connection to supply " + Math.round(flow) + " gpm",
					CheckStatus.INFO, pressure, null, "psi",
					"Starts from " + Math.round(pressureAtSystem) + " psi at the system end. The fire department pumper must be able to deliver "
							+ "this (10.4.4)."));
			checks.add(new StandpipeCheck("10.2.1", line.label() + ": pressure at the connection not above 400 psi",
					pressure <= MAXIMUM_PRESSURE_PSI ? CheckStatus.PASS : CheckStatus.FAIL, pressure, MAXIMUM_PRESSURE_PSI, "psi", null));
			checks.add(new StandpipeCheck("10.2.2", line.label() + ": pressure required at the connection not above the working pressure of the components",
					CheckStatus.CONFIRM, pressure, null, "psi", "Compare with the working pressure of the piping, valves and the connection."));
			pumperChecks(line.label(), pressure, flow, inletSize, checks);
			if (line.inlets() != null) {
				double capacity = line.inlets() * inletSize.flowPerInletGpm();
				checks.add(new StandpipeCheck(inletSize.clause(), line.label() + ": " + inletSize.label() + " inlets, one per "
						+ (long) inletSize.flowPerInletGpm() + " gpm, carry the flow of the connection",
						capacity >= flow ? CheckStatus.PASS : CheckStatus.FAIL, flow, capacity, "gpm", null));
			}
		}

		if (sharedFlow) {
			checks.add(new StandpipeCheck("10.7.3.2", "The fire department connections together carry the system demand",
					carried >= flowAtSupply - 1e-9 ? CheckStatus.PASS : CheckStatus.FAIL, carried, flowAtSupply, "gpm",
					"Where several connections share the demand, 10.7.3.2 and 10.7.3.3 allow the inlets to be on more than one connection or "
							+ "location, as the AHJ allows."));
		}
		return List.copyOf(lines);
	}

	/**
	 * A manual system has no attached supply: the calculation ends at the fire department connection and the
	 * pressure found is what the fire department must pump in (10.2.6.1.2).
	 */
	private void manualChecks(double pressureAtFdc, double flowAtSupply, boolean checkComponents, List<StandpipeCheck> checks) {
		checks.add(new StandpipeCheck("10.2.6.1.2", "Manual system: 100 psi at the most remote 2.5 in. hose connection, calculated back to the fire department connection",
				CheckStatus.INFO, pressureAtFdc, null, "psi",
				checkComponents
						? "The pressure at the supply is the pressure required at the fire department connection, at " + Math.round(flowAtSupply) + " gpm."
						: "The highest pressure required at any fire department connection given, at " + Math.round(flowAtSupply) + " gpm."));
		checks.add(new StandpipeCheck("10.4.4", "The local fire department is consulted on the water supply its apparatus can deliver",
				CheckStatus.CONFIRM, pressureAtFdc, null, "psi",
				"Confirm the pumpers can deliver " + Math.round(flowAtSupply) + " gpm at this pressure."));
		// With the connections' own piping given, each already has this check.
		if (checkComponents) {
			checks.add(new StandpipeCheck("10.2.2", "Pressure required at the fire department connection not above the working pressure of the components",
					CheckStatus.CONFIRM, pressureAtFdc, null, "psi", "Compare with the working pressure of the piping, valves and the fire department connection."));
		}
	}

	/**
	 * A.10.7 and A.10.7.3.1.3, for the pressure the fire department must deliver at a connection. The first
	 * compares it with what one 1,000 gpm pumper delivers at the NFPA 1901 rating points: it is the Annex's
	 * conservative basis, not a limit (the sign at the connection may give another pressure), so a pressure
	 * above it is confirmed against the local apparatus rather than failed. The second applies to 4 in. and
	 * 5 in. inlets, whose supply hose is rated 185 psi.
	 */
	private void pumperChecks(String who, double pressure, double flow, InletSize inletSize, List<StandpipeCheck> checks) {
		Double available = null;
		if (flow <= PUMPER_RATED_FLOW_GPM + 1e-9) {
			for (double[] point : PUMPER_RATING_POINTS_GPM_PSI) {
				if (flow <= point[0] + 1e-9) {
					available = point[1];
					break;
				}
			}
		}
		String basis = " The Annex's conservative basis is a 1,000 gpm pumper at the NFPA 1901 rating points: 1,000 gpm at 150 psi, 700 gpm at 200 psi "
				+ "and 500 gpm at 250 psi. It is not a limit; the local fire department's apparatus governs (A.10.7).";
		if (available == null) {
			checks.add(new StandpipeCheck("A.10.7", who + ": flow within one 1,000 gpm pumper",
					CheckStatus.CONFIRM, flow, PUMPER_RATED_FLOW_GPM, "gpm",
					"More than one pumper, or a larger one, is needed to supply " + Math.round(flow) + " gpm." + basis));
		} else {
			checks.add(new StandpipeCheck("A.10.7", who + ": pressure required at the connection within what one 1,000 gpm pumper delivers at " + Math.round(flow) + " gpm",
					pressure <= available + 1e-9 ? CheckStatus.PASS : CheckStatus.CONFIRM, pressure, available, "psi", basis.trim()));
		}
		if (inletSize.largeDiameter()) {
			checks.add(new StandpipeCheck("A.10.7.3.1.3", who + ": pressure required at the connection not above the " + (long) LARGE_HOSE_WORKING_PRESSURE_PSI
					+ " psi working pressure of 4 in. and larger supply hose",
					pressure <= LARGE_HOSE_WORKING_PRESSURE_PSI + 1e-9 ? CheckStatus.PASS : CheckStatus.CONFIRM, pressure, LARGE_HOSE_WORKING_PRESSURE_PSI, "psi",
					"Above this, 2½ in. inlets are used unless hose rated for the higher pressure is approved (NFPA 1961)."));
		}
	}

	private record Demand(List<DemandLine> lines, double totalGpm, double mostRemoteGpm, boolean capped) {
	}

	/** 10.6: the flow the system must deliver, built up the way the clauses do. */
	private Demand demand(StandpipeHydraulicInput input, List<StandpipeCheck> checks) {
		List<DemandLine> lines = new ArrayList<>();
		if (input.standpipeClass() == StandpipeClass.CLASS_II) {
			lines.add(new DemandLine("10.6.2.1.1", "Class II: most remote hose connection", 100));
			if (input.additionalStandpipes() > 0) {
				checks.add(new StandpipeCheck("10.6.2.1.2", "Class II: no additional flow where more than one hose connection is provided",
						CheckStatus.INFO, null, null, null, "The additional standpipes add no flow."));
			}
			return new Demand(lines, 100, 100, false);
		}

		double base = input.horizontalStandpipeOnThreeConnections() ? 750 : 500;
		lines.add(input.horizontalStandpipeOnThreeConnections()
				? new DemandLine("10.6.1.1.3", "Horizontal standpipe serving three or more connections on a floor", 750)
				: new DemandLine("10.6.1.1.1", "Most remote standpipe: 250 gpm at each of the two most remote 2.5 in. connections", 500));

		if (input.risersEndAtDifferentLevels()) {
			checks.add(new StandpipeCheck("10.6.1.2.1.1", "Standpipes ending at different floor levels need a separate calculation for each level",
					CheckStatus.CONFIRM, null, null, null,
					"This calculation follows one path. Run it again for the standpipes that exist on each level, and in each run count only "
							+ "the standpipes that exist on that level as additional standpipes (10.6.1.2.1.2)."));
		}

		// A lateral serving a single hose connection is treated as if that connection were on a separate standpipe (10.6.1.1.7).
		int laterals = input.singleConnectionLaterals();
		double additional = 0;
		int extra = input.additionalStandpipes() + laterals;
		String lateralNote = laterals > 0 ? ", of which " + laterals + " single-connection lateral(s) counted as separate standpipes (10.6.1.1.7)" : "";
		if (extra > 0 && !input.floorAreaOver80000SqFt()) {
			additional = 250.0 * extra;
			lines.add(new DemandLine("10.6.1.1.4", extra + " additional standpipe(s) at 250 gpm each" + lateralNote, additional));
		} else if (extra > 0) {
			additional = 500;
			lines.add(new DemandLine("10.6.1.1.4.1", "Floors over 80,000 sq ft: 500 gpm for the second standpipe" + lateralNote, 500));
			if (extra >= 2 && input.sprinklerProtection() == SprinklerProtection.NONE) {
				additional += 250;
				lines.add(new DemandLine("10.6.1.1.4.1", "Third standpipe, unsprinklered building: 250 gpm", 250));
			} else if (extra >= 2) {
				checks.add(new StandpipeCheck("10.6.1.1.4.1", "Floors over 80,000 sq ft with more than one additional standpipe",
						CheckStatus.CONFIRM, null, null, null,
						"The printed rule gives 250 gpm for the third standpipe only where the additional flow is required for an unsprinklered building; confirm what applies here."));
			}
		}

		double standpipeFlow = base + additional;
		double maximum = input.sprinklerProtection() == SprinklerProtection.FULL ? 1000 : 1250;
		boolean capped = standpipeFlow > maximum;
		if (capped) {
			standpipeFlow = maximum;
			lines.add(new DemandLine("10.6.1.1.6", "Limited to the maximum flow for the building", maximum));
			checks.add(new StandpipeCheck("10.6.1.1.6", "Standpipe flow limited to the maximum for the building", CheckStatus.CONFIRM,
					base + additional, maximum, "gpm", null));
		}

		double total = standpipeFlow;
		Double sprinkler = input.sprinklerDemandGpm();
		if (sprinkler != null && input.sprinklerProtection() == SprinklerProtection.FULL) {
			if (sprinkler > standpipeFlow) {
				total = sprinkler;
				lines.add(new DemandLine("10.6.1.3.1", "Sprinkler demand, including hose stream, is greater and governs", sprinkler));
			} else {
				lines.add(new DemandLine("10.6.1.3.1", "Standpipe demand is greater than the sprinkler demand and governs", standpipeFlow));
			}
		} else if (sprinkler != null && input.sprinklerProtection() == SprinklerProtection.PARTIAL) {
			double allowance = Math.min(sprinkler, input.sprinklerHazard().allowanceCapGpm());
			total = standpipeFlow + allowance;
			lines.add(new DemandLine("10.6.1.3.3", "Partly sprinklered: add the lesser of the sprinkler demand and "
					+ (long) input.sprinklerHazard().allowanceCapGpm() + " gpm", allowance));
		}
		return new Demand(lines, total, base, capped);
	}

	private void connectionChecks(StandpipeHydraulicInput input, StandpipeSegment segment, double pressure, List<StandpipeCheck> checks) {
		HoseConnectionSize size = segment.hoseConnectionSize();
		String name = segment.label();
		if (input.standpipeClass() == StandpipeClass.CLASS_I && size == HoseConnectionSize.ONE_AND_HALF_INCH) {
			checks.add(new StandpipeCheck("8.2.1", name + ": a Class I system has 2.5 in. hose connections", CheckStatus.FAIL, null, null, null, null));
		}
		checks.add(new StandpipeCheck("10.6.3", name + ": flow not above the most a connection of this size is required to deliver",
				segment.flowAddedGpm() <= size.maximumFlowGpm() ? CheckStatus.PASS : CheckStatus.FAIL,
				segment.flowAddedGpm(), size.maximumFlowGpm(), "gpm", null));
		checks.add(new StandpipeCheck("10.2.6.1", name + ": residual pressure at the outlet at least the minimum for its size", CheckStatus.PASS,
				pressure, size.minimumResidualPsi(), "psi", "Taken as the minimum residual, or the pressure the path gives there if that is higher."));
		if (size == HoseConnectionSize.ONE_AND_HALF_INCH && pressure > PRD_RESIDUAL_LIMIT_PSI) {
			checks.add(new StandpipeCheck("10.2.4.1", name + ": residual pressure at a 1.5 in. connection above 100 psi needs a listed pressure-regulating device",
					CheckStatus.CONFIRM, pressure, PRD_RESIDUAL_LIMIT_PSI, "psi",
					"Does not apply to a 1.5 in. outlet on a 2.5 x 1.5 in. reducer (10.2.4.1.1) or where no hose is required (10.2.4.1.2)."));
		}
	}

	private void sizeCheck(StandpipeHydraulicInput input, StandpipeSegment segment, List<StandpipeCheck> checks) {
		if (input.standpipeClass() == StandpipeClass.CLASS_II) {
			return;
		}
		double size = nominalInches(segment.nominalSize());
		if (segment.role() == PipeRole.STANDPIPE) {
			boolean partial = input.sprinklerProtection() == SprinklerProtection.PARTIAL;
			double limit = partial ? 6 : 4;
			checks.add(new StandpipeCheck(partial ? "10.3.1.2" : "10.3.1.1", segment.label() + ": standpipe at least " + (long) limit + " in."
					+ (partial ? " in a partly sprinklered building" : ""), size >= limit ? CheckStatus.PASS : CheckStatus.FAIL, size, limit, "in", null));
		} else if (segment.role() == PipeRole.BRANCH_LINE) {
			checks.add(new StandpipeCheck("10.3.1.4", segment.label() + ": branch line at least 2.5 in.", size >= 2.5 ? CheckStatus.PASS : CheckStatus.FAIL,
					size, 2.5, "in", null));
		}
	}

	private void supplyChecks(StandpipeHydraulicInput input, Demand demand, double hoseConnectionFlow, double flowAtSupply,
			double flowAdded, boolean automatic, double pressure, List<StandpipeCheck> checks) {
		checks.add(new StandpipeCheck(input.standpipeClass() == StandpipeClass.CLASS_II ? "10.6.2.1.1" : "10.6.1.1",
				"Hose connection flow on the path at least the flow required of the most remote standpipe",
				hoseConnectionFlow >= demand.mostRemoteGpm() ? CheckStatus.PASS : CheckStatus.FAIL, hoseConnectionFlow, demand.mostRemoteGpm(), "gpm", null));
		checks.add(new StandpipeCheck("10.6", "Flow at the supply at least the system demand",
				flowAtSupply >= demand.totalGpm() ? CheckStatus.PASS : CheckStatus.FAIL, flowAtSupply, demand.totalGpm(), "gpm",
				flowAtSupply < demand.totalGpm()
						? "Add the other standpipes and the sprinkler allowance as flow added at the supply."
						: automatic && flowAdded > 0
								? "The " + Math.round(flowAdded) + " gpm of other standpipes and sprinkler allowance was worked out from the "
										+ "demand rules and joins at the supply as flow only: its own pressure is not checked."
								: "Other standpipes and the sprinkler allowance enter as flow only: their own pressure is not checked."));
		checks.add(new StandpipeCheck("10.2.1", "Pressure not above 400 psi",
				pressure <= MAXIMUM_PRESSURE_PSI ? CheckStatus.PASS : CheckStatus.FAIL, pressure, MAXIMUM_PRESSURE_PSI, "psi",
				"Pump churn pressure and the city static pressure must also be kept within the limit."));
		checks.add(new StandpipeCheck("5.2", "Water supply duration at least 30 minutes",
				input.supplyDurationMinutes() >= MINIMUM_SUPPLY_MINUTES ? CheckStatus.PASS : CheckStatus.FAIL,
				input.supplyDurationMinutes(), MINIMUM_SUPPLY_MINUTES, "min", null));
	}

	private void fdcCheck(StandpipeHydraulicInput input, InletSize size, int requiredInlets, List<StandpipeCheck> checks) {
		Integer provided = input.fireDepartmentConnectionInlets();
		String description = size.label() + " fire department connection inlets, one per " + (long) size.flowPerInletGpm() + " gpm";
		String highRise = "At least two connections are needed per zone in high-rise buildings (10.7.2.2).";
		String hose = size.largeDiameter()
				? " The maximum operating pressure of the hose that supplies the connection (NFPA 1961) must be considered when choosing a large inlet (10.7.3.1.3)."
				: "";
		if (provided == null) {
			checks.add(new StandpipeCheck(size.clause(), description, CheckStatus.INFO, null, (double) requiredInlets, "inlets",
					"No inlet count given to compare with. " + highRise + hose));
		} else {
			checks.add(new StandpipeCheck(size.clause(), description,
					provided >= requiredInlets ? CheckStatus.PASS : CheckStatus.FAIL, (double) provided, (double) requiredInlets, "inlets",
					(highRise + hose).trim()));
		}
	}

	private double insideDiameterInch(StandpipeSegment segment) {
		return dimensionResolver.resolve(segment.pipeMaterial(), segment.schedule(), segment.nominalSize())
				.internalDiameter().to(StandpipeUnits.INCH).getValue().doubleValue();
	}

	/**
	 * Equivalent length of the segment's fittings, ft: the chart's value rescaled for the segment's C
	 * (11.3.2.2, which is {@code (C/120)^1.85}) and for an inside diameter other than Schedule 40
	 * steel (11.3.2.5, {@code (actual ID / Schedule 40 ID)^4.87}). An explicit length is used as given.
	 */
	private double fittingsLengthFt(StandpipeSegment segment, double insideDiameter) {
		double total = 0;
		for (StandpipeFitting fitting : segment.fittings()) {
			if (fitting.explicitEquivalentLengthFt() != null) {
				total += fitting.quantity() * fitting.explicitEquivalentLengthFt();
				continue;
			}
			StandpipeFittingRow row = referenceResolver.resolveFitting(fitting.fittingType());
			Double published = row.equivalentLengthFtByNominalSize().get(segment.nominalSize());
			if (published == null) {
				throw new CalculationException("segment " + segment.label() + ": " + row.label() + " has no published equivalent length for "
						+ segment.nominalSize() + " in.; give an explicit length");
			}
			double cAdjustment = Math.pow(segment.cFactor() / referenceResolver.cFactorBasis(), 1.85);
			total += fitting.quantity() * published * cAdjustment * diameterAdjustment(segment, insideDiameter);
		}
		return total;
	}

	private double diameterAdjustment(StandpipeSegment segment, double insideDiameter) {
		boolean steelSchedule40 = (segment.pipeMaterial().equalsIgnoreCase("BI") || segment.pipeMaterial().equalsIgnoreCase("GI"))
				&& "SCH40".equalsIgnoreCase(segment.schedule());
		if (steelSchedule40) {
			return 1.0;
		}
		double schedule40 = dimensionResolver.resolve("BI", "SCH40", segment.nominalSize())
				.internalDiameter().to(StandpipeUnits.INCH).getValue().doubleValue();
		return Math.pow(insideDiameter / schedule40, 4.87);
	}

	/** "2-1/2" gives 2.5, "4" gives 4, "3/4" gives 0.75. */
	static double nominalInches(String nominalSize) {
		try {
			double total = 0;
			for (String part : nominalSize.trim().split("-")) {
				if (part.contains("/")) {
					String[] fraction = part.split("/");
					total += Double.parseDouble(fraction[0]) / Double.parseDouble(fraction[1]);
				} else {
					total += Double.parseDouble(part);
				}
			}
			return total;
		} catch (RuntimeException e) {
			throw new CalculationException("Cannot read the nominal size: " + nominalSize);
		}
	}

}
