package com.renzoproject.calc.core.mechanical.standpipe;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.compliance.CheckStatus;
import com.renzoproject.calc.core.compliance.ComplianceCheck;
import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.HazenWilliams;
import com.renzoproject.calc.core.mechanical.pipe.PipeDimensionResolver;
import com.renzoproject.calc.core.mechanical.standpipe.StandpipeHydraulicResult.FdcLineRow;
import com.renzoproject.calc.core.mechanical.standpipe.StandpipeHydraulicResult.SegmentRow;
import java.util.ArrayList;
import java.util.List;

import static com.renzoproject.calc.core.mechanical.standpipe.StandpipeChecks.trim;

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
 * for one path. Friction and the fitting length rescaling come from the shared {@link HazenWilliams}.
 *
 * <p>Scope: one path (the most remote standpipe); flow of other standpipes and the sprinkler
 * allowance join at the supply as flow only, with no pressure check of their own.
 *
 * <p>This class works the path and the fire department connection lines; the demand is built by
 * {@link StandpipeDemand}, each pipe's bore and fitting length come from {@link StandpipePipeData},
 * and the compliance checks that read the results are in {@link StandpipeChecks}.
 */
public class StandpipeHydraulicCalculator implements Calculator<StandpipeHydraulicInput, StandpipeHydraulicResult> {

	private static final double PSI_PER_FOOT = 0.433;
	private static final double VELOCITY_COEFFICIENT = 0.4085;
	/** 10.6.1.2.1 and 10.2.6.1: the residual pressure at the connection point of each other standpipe, as at a 2.5 in. hose connection. */
	private static final double OTHER_STANDPIPE_RESIDUAL_PSI = 100.0;

	private final StandpipePipeData pipeData;

	public StandpipeHydraulicCalculator(PipeDimensionResolver dimensionResolver, StandpipeReferenceResolver referenceResolver) {
		this.pipeData = new StandpipePipeData(dimensionResolver, referenceResolver);
	}

	@Override
	public StandpipeHydraulicResult calculate(StandpipeHydraulicInput input) {
		List<ComplianceCheck> checks = new ArrayList<>();
		StandpipeDemand demand = StandpipeDemand.of(input, checks);

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
				if (segment.designResidualPressurePsi() != null) {
					required = Math.max(required, segment.designResidualPressurePsi());
				}
				if (remotePressure < required) {
					raised = !first;
					remotePressure = required;
				}
				StandpipeChecks.connectionChecks(input, segment, remotePressure, checks);
			} else if (segment.otherStandpipeConnection()) {
				// Another standpipe's flow is added at its connection point to the common supply piping, at the minimum
				// residual (10.6.1.2.1), and not balanced to a higher pressure there (10.6.1.2.4).
				if (remotePressure < OTHER_STANDPIPE_RESIDUAL_PSI) {
					raised = !first;
					remotePressure = OTHER_STANDPIPE_RESIDUAL_PSI;
				}
				checks.add(new ComplianceCheck("10.6.1.2.1", segment.label() + ": another standpipe's connection point, residual pressure at least the minimum",
						CheckStatus.PASS, remotePressure, OTHER_STANDPIPE_RESIDUAL_PSI, "psi",
						"Taken as the minimum residual, or the pressure the path gives there if that is higher. Its " + Math.round(segment.flowAddedGpm())
								+ " gpm is not balanced to the higher pressure at the connection point (10.6.1.2.4)."));
			}
			first = false;
			if (flow <= 0) {
				throw new CalculationException("segment " + segment.label() + " carries no flow: a flow must enter at or before it");
			}

			double insideDiameter = pipeData.insideDiameterInch(segment);
			double fittingsLength = pipeData.fittingsLengthFt(segment, insideDiameter);
			double perFoot = HazenWilliams.frictionLossPsiPerFoot(flow, segment.cFactor(), insideDiameter);
			double friction = perFoot * (segment.pipeLengthFt() + fittingsLength);
			double elevation = PSI_PER_FOOT * segment.elevationRiseFt();
			pressure = remotePressure + friction + elevation;
			totalFriction += friction;
			totalElevation += elevation;

			rows.add(new SegmentRow(segment.label(), segment.role(), segment.hoseConnectionSize(), segment.nominalSize(), insideDiameter,
					segment.cFactor(), segment.flowAddedGpm(), flow, VELOCITY_COEFFICIENT * flow / (insideDiameter * insideDiameter),
					segment.pipeLengthFt(), fittingsLength, perFoot, friction, segment.elevationRiseFt(), elevation, remotePressure,
					pressure, raised, segment.otherStandpipeConnection()));
			StandpipeChecks.sizeCheck(input, segment, checks);
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
		StandpipeChecks.supplyChecks(input, demand, hoseConnectionFlow, flowAtSupply, flowAdded, automatic, pressure, checks);
		InletSize inletSize = input.fireDepartmentConnectionInletSize();
		int requiredInlets = (int) Math.ceil(flowAtSupply / inletSize.flowPerInletGpm() - 1e-9);
		StandpipeChecks.fdcCheck(input, inletSize, requiredInlets, checks);
		Double staticAtLowest = input.supplyStaticPressurePsi() == null ? null
				: input.supplyStaticPressurePsi() - PSI_PER_FOOT * (input.lowestHoseConnectionHeightFt() == null ? 0.0 : input.lowestHoseConnectionHeightFt());
		StandpipeChecks.staticChecks(input, staticAtLowest, checks);
		List<FdcLineRow> fdcLines = fdcLines(input, inletSize, flowAtSupply, pressure, checks);
		Double highestFdcPressure = fdcLines.stream().map(FdcLineRow::pressureAtFdcPsi).max(Double::compare).orElse(null);
		if (input.manualSystem()) {
			StandpipeChecks.manualChecks(highestFdcPressure != null ? highestFdcPressure : pressure, flowAtSupply, fdcLines.isEmpty(), checks);
			if (fdcLines.isEmpty()) {
				// With no connection piping given, the manual system's pressure at the supply is the one the fire department pumps in.
				StandpipeChecks.pumperChecks("Fire department connection", pressure, flowAtSupply, inletSize, checks);
			}
			if (input.standpipeClass() == StandpipeClass.CLASS_III) {
				checks.add(new ComplianceCheck("A.8.2.3.3.1", "Manual wet system on a Class III standpipe: attached supply gives 100 gpm at 65 psi to the most remote 1.5 in. hose connection",
						CheckStatus.CONFIRM, null, null, null,
						"A manual wet standpipe can satisfy the demand of a Class III system as long as the water supply can provide 100 gpm at 65 psi to the "
								+ "most remote 1.5 in. hose connection. The fire department provides the rest of the demand through the fire department connection."));
			}
		}
		StandpipeChecks.highestPressureCheck(input, pressure, highestFdcPressure, checks);

		return new StandpipeHydraulicResult(demand.lines(), demand.totalGpm(), demand.capped(), demand.mostRemoteGpm(), flowAtSupply, flowAdded,
				automatic, pressure,
				totalFriction, totalElevation, requiredInlets, inletSize, input.manualSystem(), input.supplyDurationMinutes(),
				flowAtSupply * input.supplyDurationMinutes(), fdcLines, highestFdcPressure, staticAtLowest, List.copyOf(rows), List.copyOf(checks));
	}

	/**
	 * The other standpipes of a Class I or III system join the common supply piping at their connection
	 * points, with their flow at the minimum residual pressure and not balanced to a higher one
	 * (10.6.1.2.1, 10.6.1.2.4, A.10.6.1.2.3). Those not placed on the path join at the supply end, so the
	 * pressure required there is at least that residual. Returns the pressure required at the supply end.
	 */
	private double otherStandpipesAtTheSupply(StandpipeHydraulicInput input, double flowAdded, double pressure, List<ComplianceCheck> checks) {
		boolean others = input.standpipeClass() != StandpipeClass.CLASS_II
				&& input.additionalStandpipes() + input.singleConnectionLaterals() > 0 && flowAdded > 0;
		if (!others) {
			return pressure;
		}
		boolean raised = pressure < OTHER_STANDPIPE_RESIDUAL_PSI;
		double required = Math.max(pressure, OTHER_STANDPIPE_RESIDUAL_PSI);
		checks.add(new ComplianceCheck("10.6.1.2.1", "Other standpipes joining at the supply end: residual pressure at their connection point at least the minimum",
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
			List<ComplianceCheck> checks) {
		if (input.fireDepartmentConnections().isEmpty()) {
			if (input.standpipeClass() != StandpipeClass.CLASS_II) {
				checks.add(new ComplianceCheck("10.4.1", "Each fire department connection shown able to supply the system demand",
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
				double insideDiameter = pipeData.insideDiameterInch(segment);
				double fittingsLength = pipeData.fittingsLengthFt(segment, insideDiameter);
				double perFoot = HazenWilliams.frictionLossPsiPerFoot(flow, segment.cFactor(), insideDiameter);
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

			checks.add(new ComplianceCheck("10.4.1", line.label() + ": pressure required at the connection to supply " + Math.round(flow) + " gpm",
					CheckStatus.INFO, pressure, null, "psi",
					"Starts from " + Math.round(pressureAtSystem) + " psi at the system end. The fire department pumper must be able to deliver "
							+ "this (10.4.4)."));
			checks.add(new ComplianceCheck("10.2.1", line.label() + ": pressure at the connection not above 400 psi",
					pressure <= StandpipeChecks.MAXIMUM_PRESSURE_PSI ? CheckStatus.PASS : CheckStatus.FAIL, pressure, StandpipeChecks.MAXIMUM_PRESSURE_PSI, "psi", null));
			checks.add(new ComplianceCheck("10.2.2", line.label() + ": pressure required at the connection not above the working pressure of the components",
					CheckStatus.CONFIRM, pressure, null, "psi", "Compare with the working pressure of the piping, valves and the connection."));
			StandpipeChecks.pumperChecks(line.label(), pressure, flow, inletSize, checks);
			if (line.inlets() != null) {
				double capacity = line.inlets() * inletSize.flowPerInletGpm();
				checks.add(new ComplianceCheck(inletSize.clause(), line.label() + ": " + inletSize.label() + " inlets, one per "
						+ (long) inletSize.flowPerInletGpm() + " gpm, carry the flow of the connection",
						capacity >= flow ? CheckStatus.PASS : CheckStatus.FAIL, flow, capacity, "gpm", null));
			}
		}

		if (sharedFlow) {
			checks.add(new ComplianceCheck("10.7.3.2", "The fire department connections together carry the system demand",
					carried >= flowAtSupply - 1e-9 ? CheckStatus.PASS : CheckStatus.FAIL, carried, flowAtSupply, "gpm",
					"Where several connections share the demand, 10.7.3.2 and 10.7.3.3 allow the inlets to be on more than one connection or "
							+ "location, as the AHJ allows."));
		}
		return List.copyOf(lines);
	}

}
