package com.renzoproject.calc.core.mechanical.standpipe;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.PipeDimensionResolver;
import com.renzoproject.calc.core.mechanical.standpipe.StandpipeHydraulicResult.DemandLine;
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
	private static final double FDC_INLET_FLOW_GPM = 250.0;
	private static final double MAXIMUM_PRESSURE_PSI = 400.0;
	private static final double PRD_RESIDUAL_LIMIT_PSI = 100.0;
	private static final double MINIMUM_SUPPLY_MINUTES = 30.0;

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
					pressure, raised));
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
		supplyChecks(input, demand, hoseConnectionFlow, flowAtSupply, flowAdded, automatic, pressure, checks);
		int requiredInlets = (int) Math.ceil(flowAtSupply / FDC_INLET_FLOW_GPM - 1e-9);
		fdcCheck(input, requiredInlets, flowAtSupply, checks);

		return new StandpipeHydraulicResult(demand.lines(), demand.totalGpm(), demand.capped(), demand.mostRemoteGpm(), flowAtSupply, flowAdded,
				automatic, pressure,
				totalFriction, totalElevation, requiredInlets, input.supplyDurationMinutes(), flowAtSupply * input.supplyDurationMinutes(),
				List.copyOf(rows), List.copyOf(checks));
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

		double additional = 0;
		int extra = input.additionalStandpipes();
		if (extra > 0 && !input.floorAreaOver80000SqFt()) {
			additional = 250.0 * extra;
			lines.add(new DemandLine("10.6.1.1.4", extra + " additional standpipe(s) at 250 gpm each", additional));
		} else if (extra > 0) {
			additional = 500;
			lines.add(new DemandLine("10.6.1.1.4.1", "Floors over 80,000 sq ft: 500 gpm for the second standpipe", 500));
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
		checks.add(new StandpipeCheck("10.2.4.2", "Static pressure at a hose connection not above 175 psi", CheckStatus.OPEN, null, 175.0, "psi",
				"Needs the static pressure of the supply, which this calculation does not take. Above 175 psi static a listed pressure-regulating "
						+ "device is required at a 2.5 in. connection (10.2.4.2) and at a 1.5 in. connection (10.2.4.1.3)."));
		checks.add(new StandpipeCheck("5.2", "Water supply duration at least 30 minutes",
				input.supplyDurationMinutes() >= MINIMUM_SUPPLY_MINUTES ? CheckStatus.PASS : CheckStatus.FAIL,
				input.supplyDurationMinutes(), MINIMUM_SUPPLY_MINUTES, "min", null));
	}

	private void fdcCheck(StandpipeHydraulicInput input, int requiredInlets, double flowAtSupply, List<StandpipeCheck> checks) {
		Integer provided = input.fireDepartmentConnectionInlets();
		if (provided == null) {
			checks.add(new StandpipeCheck("10.7.3", "2.5 in. fire department connection inlets, one per 250 gpm", CheckStatus.INFO, null,
					(double) requiredInlets, "inlets", "No inlet count given to compare with. A 4 in. inlet counts as 500 gpm and a 5 in. inlet as "
							+ "750 gpm (10.7.3.1); at least two connections are needed per zone in high-rise buildings (10.7.2.2)."));
		} else {
			checks.add(new StandpipeCheck("10.7.3", "2.5 in. fire department connection inlets, one per 250 gpm",
					provided >= requiredInlets ? CheckStatus.PASS : CheckStatus.FAIL, (double) provided, (double) requiredInlets, "inlets", null));
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
