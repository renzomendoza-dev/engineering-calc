package com.renzoproject.calc.core.mechanical.standpipe;

import com.renzoproject.calc.core.compliance.CheckStatus;
import com.renzoproject.calc.core.compliance.ComplianceCheck;
import com.renzoproject.calc.core.mechanical.standpipe.StandpipeHydraulicResult.DemandLine;
import java.util.ArrayList;
import java.util.List;

/**
 * The flow a standpipe system must deliver (NFPA 14, 10.6), with the lines it is built up from.
 *
 * @param lines         each step of the build-up, with its clause
 * @param totalGpm      the system demand
 * @param mostRemoteGpm what the most remote standpipe alone must deliver
 * @param capped        whether the standpipe flow was limited to the building maximum (10.6.1.1.6)
 */
record StandpipeDemand(List<DemandLine> lines, double totalGpm, double mostRemoteGpm, boolean capped) {

	/** 10.6: the flow the system must deliver, built up the way the clauses do. */
	static StandpipeDemand of(StandpipeHydraulicInput input, List<ComplianceCheck> checks) {
		List<DemandLine> lines = new ArrayList<>();
		if (input.standpipeClass() == StandpipeClass.CLASS_II) {
			lines.add(new DemandLine("10.6.2.1.1", "Class II: most remote hose connection", 100));
			if (input.additionalStandpipes() > 0) {
				checks.add(new ComplianceCheck("10.6.2.1.2", "Class II: no additional flow where more than one hose connection is provided",
						CheckStatus.INFO, null, null, null, "The additional standpipes add no flow."));
			}
			return new StandpipeDemand(lines, 100, 100, false);
		}

		double base = input.horizontalStandpipeOnThreeConnections() ? 750 : 500;
		lines.add(input.horizontalStandpipeOnThreeConnections()
				? new DemandLine("10.6.1.1.3", "Horizontal standpipe serving three or more connections on a floor", 750)
				: new DemandLine("10.6.1.1.1", "Most remote standpipe: 250 gpm at each of the two most remote 2.5 in. connections", 500));

		if (input.risersEndAtDifferentLevels()) {
			checks.add(new ComplianceCheck("10.6.1.2.1.1", "Standpipes ending at different floor levels need a separate calculation for each level",
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
				checks.add(new ComplianceCheck("10.6.1.1.4.1", "Floors over 80,000 sq ft with more than one additional standpipe",
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
			checks.add(new ComplianceCheck("10.6.1.1.6", "Standpipe flow limited to the maximum for the building", CheckStatus.CONFIRM,
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
		return new StandpipeDemand(lines, total, base, capped);
	}

}
