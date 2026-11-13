package com.renzoproject.calc.core.mechanical.standpipe;

import com.renzoproject.calc.core.compliance.CheckStatus;
import com.renzoproject.calc.core.compliance.ComplianceCheck;
import java.util.List;

/**
 * The compliance checks of a standpipe calculation that read the figures the hydraulics produce:
 * static pressure, hose connections, pipe sizes, the supply, fire department connection inlets, the
 * manual system, the pumper basis of the Annex and the highest pressure. Part of
 * {@link StandpipeHydraulicCalculator}.
 */
final class StandpipeChecks {

	static final double MAXIMUM_PRESSURE_PSI = 400.0;
	/** 10.2.4.1.3 and 10.2.4.2: above this static pressure at a hose connection a listed pressure-regulating device is required. */
	private static final double STATIC_PRESSURE_LIMIT_PSI = 175.0;
	private static final double PRD_RESIDUAL_LIMIT_PSI = 100.0;
	/** A.7.1: below this highest available pressure standard-weight valves and fittings are acceptable. */
	private static final double STANDARD_WEIGHT_LIMIT_PSI = 175.0;
	private static final double MINIMUM_SUPPLY_MINUTES = 30.0;
	/**
	 * A.10.7: the conservative basis for what the fire department can deliver, a 1,000 gpm pumper at the NFPA 1901
	 * rating points: 1,000 gpm at 150 psi, 700 gpm at 200 psi and 500 gpm at 250 psi.
	 */
	private static final double PUMPER_RATED_FLOW_GPM = 1000.0;
	private static final double[][] PUMPER_RATING_POINTS_GPM_PSI = {{500.0, 250.0}, {700.0, 200.0}, {1000.0, 150.0}};
	/** A.10.7.3.1.3: the maximum working pressure of 4 in. and larger supply hose (NFPA 1961). */
	private static final double LARGE_HOSE_WORKING_PRESSURE_PSI = 185.0;

	private StandpipeChecks() {
	}

	/** A figure for a note: whole when it is whole, otherwise to a tenth. */
	static String trim(double value) {
		return value == Math.rint(value) ? String.valueOf((long) value) : String.format(java.util.Locale.ROOT, "%.1f", value);
	}

	/**
	 * The static (nonflowing) pressure limits: 400 psi anywhere in the system at any time (10.2.1), and
	 * 175 psi at a hose connection, above which a listed pressure-regulating device is required
	 * (10.2.4.2 for 2.5 in., 10.2.4.1.3 for 1.5 in.). The highest static pressure at a hose connection
	 * is at the lowest one, the supply static pressure less the head of its height above the supply.
	 */
	static void staticChecks(StandpipeHydraulicInput input, Double staticAtLowest, List<ComplianceCheck> checks) {
		Double supplyStatic = input.supplyStaticPressurePsi();
		if (supplyStatic == null) {
			checks.add(new ComplianceCheck("10.2.4.2", "Static pressure at the lowest hose connection not above 175 psi", CheckStatus.OPEN, null,
					STATIC_PRESSURE_LIMIT_PSI, "psi",
					"Give the static pressure at the supply. For a fire pump it is the pump boost at churn plus the supply pressure at the "
							+ "suction (A.10.2). Above 175 psi static a listed pressure-regulating device is required at a 2.5 in. connection "
							+ "(10.2.4.2) and at a 1.5 in. connection (10.2.4.1.3)."));
			return;
		}
		checks.add(new ComplianceCheck("10.2.1", "Static pressure at the supply not above 400 psi", supplyStatic <= MAXIMUM_PRESSURE_PSI ? CheckStatus.PASS
				: CheckStatus.FAIL, supplyStatic, MAXIMUM_PRESSURE_PSI, "psi",
				"The maximum system pressure is normally at pump churn and includes both the pump boost and the supply pressure (A.10.2). "
						+ "Express mains to vertical zones may exceed 400 psi where their listings allow, with no hose valves on them (10.2.3)."));
		boolean above = staticAtLowest > STATIC_PRESSURE_LIMIT_PSI;
		double height = input.lowestHoseConnectionHeightFt() == null ? 0.0 : input.lowestHoseConnectionHeightFt();
		String basis = "Supply static " + Math.round(supplyStatic) + " psi, less 0.433 psi per foot over " + trim(height) + " ft.";
		checks.add(new ComplianceCheck("10.2.4.2", "Static pressure at the lowest hose connection not above 175 psi",
				above ? CheckStatus.CONFIRM : CheckStatus.PASS, staticAtLowest, STATIC_PRESSURE_LIMIT_PSI, "psi",
				above
						? basis + " A listed pressure-regulating device is required to limit the static and residual pressure at the hose "
								+ "connection to 175 psi, at a 2.5 in. connection (10.2.4.2) and at a 1.5 in. connection (10.2.4.1.3). The annex says "
								+ "that on a Class I standpipe one should not be required where the hose and nozzles of the responding fire "
								+ "company are rated for the pressures and the AHJ approves (A.10.2.4)."
						: basis));
	}

	/**
	 * A manual system has no attached supply: the calculation ends at the fire department connection and the
	 * pressure found is what the fire department must pump in (10.2.6.1.2).
	 */
	static void manualChecks(double pressureAtFdc, double flowAtSupply, boolean checkComponents, List<ComplianceCheck> checks) {
		checks.add(new ComplianceCheck("10.2.6.1.2", "Manual system: 100 psi at the most remote 2.5 in. hose connection, calculated back to the fire department connection",
				CheckStatus.INFO, pressureAtFdc, null, "psi",
				checkComponents
						? "The pressure at the supply is the pressure required at the fire department connection, at " + Math.round(flowAtSupply) + " gpm."
						: "The highest pressure required at any fire department connection given, at " + Math.round(flowAtSupply) + " gpm."));
		checks.add(new ComplianceCheck("10.4.4", "The local fire department is consulted on the water supply its apparatus can deliver",
				CheckStatus.CONFIRM, pressureAtFdc, null, "psi",
				"Confirm the pumpers can deliver " + Math.round(flowAtSupply) + " gpm at this pressure."));
		// With the connections' own piping given, each already has this check.
		if (checkComponents) {
			checks.add(new ComplianceCheck("10.2.2", "Pressure required at the fire department connection not above the working pressure of the components",
					CheckStatus.CONFIRM, pressureAtFdc, null, "psi", "Compare with the working pressure of the piping, valves and the fire department connection."));
		}
	}

	/**
	 * A.7.1: standard-weight valves and fittings are ordinarily confined to the upper stories of very high
	 * buildings and to equipment in which the highest available pressure is below 175 psi. The highest
	 * pressure is taken from the supply static pressure where given, the pressure required at the supply
	 * and the pressure required at any fire department connection.
	 */
	static void highestPressureCheck(StandpipeHydraulicInput input, double pressureAtSupply, Double highestFdcPressure, List<ComplianceCheck> checks) {
		double highest = pressureAtSupply;
		String where = "the pressure required at the supply";
		if (highestFdcPressure != null && highestFdcPressure > highest) {
			highest = highestFdcPressure;
			where = "the pressure required at a fire department connection";
		}
		if (input.supplyStaticPressurePsi() != null && input.supplyStaticPressurePsi() > highest) {
			highest = input.supplyStaticPressurePsi();
			where = "the static pressure at the supply";
		}
		boolean below = highest < STANDARD_WEIGHT_LIMIT_PSI;
		checks.add(new ComplianceCheck("A.7.1", "Highest pressure in the system below 175 psi, where standard-weight valves and fittings are acceptable",
				below ? CheckStatus.PASS : CheckStatus.CONFIRM, highest, STANDARD_WEIGHT_LIMIT_PSI, "psi",
				below ? "The highest is " + where + "."
						: "The highest is " + where + ". Use valves and fittings rated for it: standard-weight ones are confined to the upper stories of "
								+ "very high buildings and to equipment where the highest available pressure is below 175 psi."));
	}

	/**
	 * A.10.7 and A.10.7.3.1.3, for the pressure the fire department must deliver at a connection. The first
	 * compares it with what one 1,000 gpm pumper delivers at the NFPA 1901 rating points: it is the Annex's
	 * conservative basis, not a limit (the sign at the connection may give another pressure), so a pressure
	 * above it is confirmed against the local apparatus rather than failed. The second applies to 4 in. and
	 * 5 in. inlets, whose supply hose is rated 185 psi.
	 */
	static void pumperChecks(String who, double pressure, double flow, InletSize inletSize, List<ComplianceCheck> checks) {
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
			checks.add(new ComplianceCheck("A.10.7", who + ": flow within one 1,000 gpm pumper",
					CheckStatus.CONFIRM, flow, PUMPER_RATED_FLOW_GPM, "gpm",
					"More than one pumper, or a larger one, is needed to supply " + Math.round(flow) + " gpm." + basis));
		} else {
			checks.add(new ComplianceCheck("A.10.7", who + ": pressure required at the connection within what one 1,000 gpm pumper delivers at " + Math.round(flow) + " gpm",
					pressure <= available + 1e-9 ? CheckStatus.PASS : CheckStatus.CONFIRM, pressure, available, "psi", basis.trim()));
		}
		if (inletSize.largeDiameter()) {
			checks.add(new ComplianceCheck("A.10.7.3.1.3", who + ": pressure required at the connection not above the " + (long) LARGE_HOSE_WORKING_PRESSURE_PSI
					+ " psi working pressure of 4 in. and larger supply hose",
					pressure <= LARGE_HOSE_WORKING_PRESSURE_PSI + 1e-9 ? CheckStatus.PASS : CheckStatus.CONFIRM, pressure, LARGE_HOSE_WORKING_PRESSURE_PSI, "psi",
					"Above this, 2½ in. inlets are used unless hose rated for the higher pressure is approved (NFPA 1961)."));
		}
	}

	static void connectionChecks(StandpipeHydraulicInput input, StandpipeSegment segment, double pressure, List<ComplianceCheck> checks) {
		HoseConnectionSize size = segment.hoseConnectionSize();
		String name = segment.label();
		if (input.standpipeClass() == StandpipeClass.CLASS_I && size == HoseConnectionSize.ONE_AND_HALF_INCH) {
			checks.add(new ComplianceCheck("8.2.1", name + ": a Class I system has 2.5 in. hose connections", CheckStatus.FAIL, null, null, null, null));
		}
		checks.add(new ComplianceCheck("10.6.3", name + ": flow not above the most a connection of this size is required to deliver",
				segment.flowAddedGpm() <= size.maximumFlowGpm() ? CheckStatus.PASS : CheckStatus.FAIL,
				segment.flowAddedGpm(), size.maximumFlowGpm(), "gpm", null));
		checks.add(new ComplianceCheck("10.2.6.1", name + ": residual pressure at the outlet at least the minimum for its size", CheckStatus.PASS,
				pressure, size.minimumResidualPsi(), "psi", "Taken as the minimum residual, or the pressure the path gives there if that is higher."));
		Double design = segment.designResidualPressurePsi();
		if (design != null) {
			boolean small = size == HoseConnectionSize.ONE_AND_HALF_INCH;
			double limit = small ? PRD_RESIDUAL_LIMIT_PSI : STATIC_PRESSURE_LIMIT_PSI;
			checks.add(new ComplianceCheck("A.10.2.6", name + ": residual pressure at the outlet held at the pressure the designer gave for the fire department's hose and nozzle",
					CheckStatus.INFO, pressure, design, "psi",
					design <= size.minimumResidualPsi()
							? "Not above the " + trim(size.minimumResidualPsi()) + " psi minimum for this size, so the minimum applies (10.2.6.1)."
							: "Above the " + trim(size.minimumResidualPsi()) + " psi minimum for this size (10.2.6.1), so the outlet is held at it. How much pressure a "
									+ "hose and nozzle need varies with the hose, and the fire department should flow test its own choice (A.10.2.6)."));
			checks.add(new ComplianceCheck(small ? "10.2.4.1" : "10.2.4.2",
					name + ": pressure required at the outlet not above " + trim(limit) + " psi, to which a pressure-regulating device limits the residual pressure",
					design <= limit ? CheckStatus.PASS : CheckStatus.FAIL, design, limit, "psi",
					design <= limit ? null
							: "The pressure at an outlet is never below its flowing pressure, so a connection that needs " + trim(design)
									+ " psi flowing has more than " + trim(limit) + " psi static and needs a listed pressure-regulating device that limits "
									+ "static and residual pressure to " + trim(limit) + " psi: the equipment cannot be given what it needs there."));
		}
		if (size == HoseConnectionSize.ONE_AND_HALF_INCH && pressure > PRD_RESIDUAL_LIMIT_PSI) {
			checks.add(new ComplianceCheck("10.2.4.1", name + ": residual pressure at a 1.5 in. connection above 100 psi needs a listed pressure-regulating device",
					CheckStatus.CONFIRM, pressure, PRD_RESIDUAL_LIMIT_PSI, "psi",
					"Does not apply to a 1.5 in. outlet on a 2.5 x 1.5 in. reducer (10.2.4.1.1) or where no hose is required (10.2.4.1.2)."));
		}
	}

	static void sizeCheck(StandpipeHydraulicInput input, StandpipeSegment segment, List<ComplianceCheck> checks) {
		if (input.standpipeClass() == StandpipeClass.CLASS_II) {
			return;
		}
		double size = StandpipePipeData.nominalInches(segment.nominalSize());
		if (segment.role() == PipeRole.STANDPIPE) {
			boolean partial = input.sprinklerProtection() == SprinklerProtection.PARTIAL;
			double limit = partial ? 6 : 4;
			checks.add(new ComplianceCheck(partial ? "10.3.1.2" : "10.3.1.1", segment.label() + ": standpipe at least " + (long) limit + " in."
					+ (partial ? " in a partly sprinklered building" : ""), size >= limit ? CheckStatus.PASS : CheckStatus.FAIL, size, limit, "in", null));
		} else if (segment.role() == PipeRole.BRANCH_LINE) {
			checks.add(new ComplianceCheck("10.3.1.4", segment.label() + ": branch line at least 2.5 in.", size >= 2.5 ? CheckStatus.PASS : CheckStatus.FAIL,
					size, 2.5, "in", null));
		}
	}

	static void supplyChecks(StandpipeHydraulicInput input, StandpipeDemand demand, double hoseConnectionFlow, double flowAtSupply,
			double flowAdded, boolean automatic, double pressure, List<ComplianceCheck> checks) {
		checks.add(new ComplianceCheck(input.standpipeClass() == StandpipeClass.CLASS_II ? "10.6.2.1.1" : "10.6.1.1",
				"Hose connection flow on the path at least the flow required of the most remote standpipe",
				hoseConnectionFlow >= demand.mostRemoteGpm() ? CheckStatus.PASS : CheckStatus.FAIL, hoseConnectionFlow, demand.mostRemoteGpm(), "gpm", null));
		checks.add(new ComplianceCheck("10.6", "Flow at the supply at least the system demand",
				flowAtSupply >= demand.totalGpm() ? CheckStatus.PASS : CheckStatus.FAIL, flowAtSupply, demand.totalGpm(), "gpm",
				flowAtSupply < demand.totalGpm()
						? "Add the other standpipes and the sprinkler allowance as flow added at the supply."
						: automatic && flowAdded > 0
								? "The " + Math.round(flowAdded) + " gpm of other standpipes and sprinkler allowance was worked out from the "
										+ "demand rules and joins at the supply as flow only: its own pressure is not checked."
								: "Other standpipes and the sprinkler allowance enter as flow only: their own pressure is not checked."));
		checks.add(new ComplianceCheck("10.2.1", "Pressure not above 400 psi",
				pressure <= MAXIMUM_PRESSURE_PSI ? CheckStatus.PASS : CheckStatus.FAIL, pressure, MAXIMUM_PRESSURE_PSI, "psi",
				"Pump churn pressure and the city static pressure must also be kept within the limit."));
		checks.add(new ComplianceCheck("5.2", "Water supply duration at least 30 minutes",
				input.supplyDurationMinutes() >= MINIMUM_SUPPLY_MINUTES ? CheckStatus.PASS : CheckStatus.FAIL,
				input.supplyDurationMinutes(), MINIMUM_SUPPLY_MINUTES, "min", null));
	}

	static void fdcCheck(StandpipeHydraulicInput input, InletSize size, int requiredInlets, List<ComplianceCheck> checks) {
		Integer provided = input.fireDepartmentConnectionInlets();
		String description = size.label() + " fire department connection inlets, one per " + (long) size.flowPerInletGpm() + " gpm";
		String highRise = "At least two connections are needed per zone in high-rise buildings (10.7.2.2).";
		String hose = size.largeDiameter()
				? " The maximum operating pressure of the hose that supplies the connection (NFPA 1961) must be considered when choosing a large inlet (10.7.3.1.3)."
				: "";
		if (provided == null) {
			checks.add(new ComplianceCheck(size.clause(), description, CheckStatus.INFO, null, (double) requiredInlets, "inlets",
					"No inlet count given to compare with. " + highRise + hose));
		} else {
			checks.add(new ComplianceCheck(size.clause(), description,
					provided >= requiredInlets ? CheckStatus.PASS : CheckStatus.FAIL, (double) provided, (double) requiredInlets, "inlets",
					(highRise + hose).trim()));
		}
	}

}
