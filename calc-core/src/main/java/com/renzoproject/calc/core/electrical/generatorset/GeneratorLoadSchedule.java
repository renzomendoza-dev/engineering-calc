package com.renzoproject.calc.core.electrical.generatorset;

import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetReference.AreaType;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetReference.DemandGroup;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetReference.DemandTier;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetReference.Occupancy;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.DirectLoad;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.LightingArea;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.MotorLoad;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.OtherLoad;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.ReceptacleGroup;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingResult.LoadRow;
import com.renzoproject.calc.core.electrical.motorflc.MotorFlcCalculator;
import com.renzoproject.calc.core.electrical.motorflc.MotorFlcInput;
import com.renzoproject.calc.core.electrical.reference.MotorClass;
import com.renzoproject.calc.core.electrical.reference.MotorPhaseType;
import com.renzoproject.calc.core.exception.CalculationException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.renzoproject.calc.core.electrical.generatorset.GeneratorSetText.labelOf;
import static com.renzoproject.calc.core.electrical.generatorset.GeneratorSetText.num;

/**
 * The load schedule of a generator set, built the way PEC 2017 Article 2.20 builds a feeder load:
 * lighting by occupancy with the Table 2.20.3.3 demand factors, receptacles with Table 2.20.3.5,
 * other loads with their own demand factor, motors from the Article 4.30 full-load current tables,
 * and loads entered directly as a figure. Part of {@link GeneratorSetSizingCalculator}.
 */
final class GeneratorLoadSchedule {

	private final GeneratorSetReference reference;
	private final MotorFlcCalculator motorFlcCalculator;

	GeneratorLoadSchedule(GeneratorSetReference reference, MotorFlcCalculator motorFlcCalculator) {
		this.reference = reference;
		this.motorFlcCalculator = motorFlcCalculator;
	}

	/** The load in volt-amperes: kVA and kW (through the power factor) as given, amperes at the system voltage. */
	static double directVoltAmperes(DirectLoad load, double phaseFactor, int voltage) {
		return switch (load.unit()) {
			case KVA -> load.amount() * 1000.0;
			case KW -> load.amount() * 1000.0 / load.powerFactor();
			case AMPS -> phaseFactor * voltage * load.amount();
		};
	}

	List<LoadRow> directRows(List<DirectLoad> loads, double phaseFactor, int voltage) {
		List<LoadRow> rows = new ArrayList<>();
		for (DirectLoad load : loads) {
			double va = directVoltAmperes(load, phaseFactor, voltage);
			String entered = switch (load.unit()) {
				case KVA -> String.format(Locale.ROOT, "%s kVA", num(load.amount()));
				case KW -> String.format(Locale.ROOT, "%s kW at %.2f power factor = %s kVA", num(load.amount()),
						load.powerFactor(), num(va / 1000.0));
				case AMPS -> String.format(Locale.ROOT, "%s A at %d V = %s kVA", num(load.amount()), voltage, num(va / 1000.0));
			};
			String basis = "Total building load entered as a figure: " + entered
					+ ", taken at 100%. Enter it after your own demand factors; none is applied here.";
			rows.add(new LoadRow(LoadKind.BUILDING, labelOf(load.label(), "Building load"),
					va, 100.0, va, load.continuous(), basis));
		}
		return rows;
	}

	List<LoadRow> lightingRows(List<LightingArea> areas) {
		record Resolved(LightingArea area, Occupancy occupancy, DemandGroup group, double unitLoad, double connectedVa, boolean fullDemand) {
		}
		List<Resolved> resolved = new ArrayList<>();
		for (LightingArea area : areas) {
			Occupancy occupancy = reference.occupancy(area.occupancy());
			AreaType areaType = reference.areaType(area.areaType());
			DemandGroup group = reference.demandGroup(occupancy.demandGroup());
			double tableUnitLoad = areaType.voltAmperesPerM2() != null ? areaType.voltAmperesPerM2() : occupancy.voltAmperesPerM2();
			double unitLoad = tableUnitLoad;
			if (area.unitLoadVoltAmperesPerM2() != null) {
				if (area.unitLoadVoltAmperesPerM2() < tableUnitLoad) {
					throw new CalculationException(String.format(Locale.ROOT,
							"The lighting unit load for %s cannot be below the Table 2.20.2.3 minimum of %s VA/m2 (2.20.2.3)",
							labelOf(area.label(), occupancy.label()), num(tableUnitLoad)));
				}
				unitLoad = area.unitLoadVoltAmperesPerM2();
			}
			boolean fullDemand = area.entireLightingUsedAtOnce() && group.fullDemandWhenAllLightingInUse();
			resolved.add(new Resolved(area, occupancy, group, unitLoad, area.floorAreaM2() * unitLoad, fullDemand));
		}

		// The stepped factors of Table 2.20.3.3 apply to the total lighting load of one occupancy
		// type, so areas of the same group are pooled before the tiers are walked.
		Map<String, Double> pooledVa = new LinkedHashMap<>();
		for (Resolved r : resolved) {
			if (!r.fullDemand()) {
				pooledVa.merge(r.group().key(), r.connectedVa(), Double::sum);
			}
		}
		Map<String, Double> effectivePercent = new LinkedHashMap<>();
		for (Map.Entry<String, Double> entry : pooledVa.entrySet()) {
			DemandGroup group = reference.demandGroup(entry.getKey());
			double demand = tierDemand(entry.getValue(), group.tiers());
			effectivePercent.put(entry.getKey(), demand / entry.getValue() * 100.0);
		}

		List<LoadRow> rows = new ArrayList<>();
		for (Resolved r : resolved) {
			double percent = r.fullDemand() ? 100.0 : effectivePercent.get(r.group().key());
			String basis = String.format(Locale.ROOT, "Table 2.20.2.3: %s VA/m2 x %s m2 (%s). ",
					num(r.unitLoad()), num(r.area().floorAreaM2()), r.occupancy().label());
			if (r.fullDemand()) {
				basis += "Table 2.20.3.3 note: the demand factors do not apply where the entire lighting is used at one time, so 100%.";
			} else if (r.group().tiers().size() == 1) {
				basis += "Table 2.20.3.3 (" + r.group().label() + "): no reduction, " + num(percent) + "%.";
			} else {
				basis += "Table 2.20.3.3 (" + r.group().label() + "): " + describeTiers(r.group().tiers())
						+ String.format(Locale.ROOT, "; %s%% on the %s VA of this occupancy.", num(percent), num(pooledVa.get(r.group().key())));
			}
			rows.add(new LoadRow(LoadKind.LIGHTING, labelOf(r.area().label(), r.occupancy().label()),
					r.connectedVa(), percent, r.connectedVa() * percent / 100.0, r.area().continuous(), basis));
		}
		return rows;
	}

	List<LoadRow> receptacleRows(List<ReceptacleGroup> groups, boolean applyDemandFactor) {
		if (groups.isEmpty()) {
			return List.of();
		}
		double perOutlet = reference.receptacleVoltAmperesPerOutlet();
		double[] connected = new double[groups.size()];
		String[] basis = new String[groups.size()];
		double total = 0;
		for (int i = 0; i < groups.size(); i++) {
			ReceptacleGroup group = groups.get(i);
			double byOutlets = group.outlets() * perOutlet;
			connected[i] = byOutlets;
			basis[i] = String.format(Locale.ROOT, "2.20.2.5(I): %d outlets x %s VA.", group.outlets(), num(perOutlet));
			if (group.bankOrOfficeFloorAreaM2() != null) {
				double byArea = group.bankOrOfficeFloorAreaM2() * reference.bankOfficeReceptacleVoltAmperesPerM2();
				basis[i] = String.format(Locale.ROOT,
						"2.20.2.5(K): the larger of %d outlets x %s VA = %s VA and %s m2 x %s VA/m2 = %s VA.",
						group.outlets(), num(perOutlet), num(byOutlets), num(group.bankOrOfficeFloorAreaM2()),
						num(reference.bankOfficeReceptacleVoltAmperesPerM2()), num(byArea));
				connected[i] = Math.max(byOutlets, byArea);
			}
			total += connected[i];
		}
		double percent = 100.0;
		String demandText = " No demand factor applied (100%).";
		if (applyDemandFactor) {
			percent = tierDemand(total, reference.receptacleDemandTiers()) / total * 100.0;
			demandText = " Table 2.20.3.5: " + describeTiers(reference.receptacleDemandTiers())
					+ String.format(Locale.ROOT, "; %s%% on the %s VA of receptacle load.", num(percent), num(total));
		}
		List<LoadRow> rows = new ArrayList<>();
		for (int i = 0; i < groups.size(); i++) {
			ReceptacleGroup group = groups.get(i);
			rows.add(new LoadRow(LoadKind.RECEPTACLE, labelOf(group.label(), "Receptacle outlets"),
					connected[i], percent, connected[i] * percent / 100.0, group.continuous(), basis[i] + demandText));
		}
		return rows;
	}

	List<LoadRow> otherRows(List<OtherLoad> loads) {
		List<LoadRow> rows = new ArrayList<>();
		for (OtherLoad load : loads) {
			rows.add(new LoadRow(LoadKind.OTHER, labelOf(load.label(), "Other load"),
					load.voltAmperes(), load.demandFactorPercent(),
					load.voltAmperes() * load.demandFactorPercent() / 100.0, load.continuous(),
					String.format(Locale.ROOT, "Entered load with a %s%% demand factor.", num(load.demandFactorPercent()))));
		}
		return rows;
	}

	/** A motor entry as the load schedule names it, with its running load, kept for the starting check. */
	record MotorEntry(MotorLoad motor, String label, double runningVa) {
	}

	record MotorRows(List<LoadRow> rows, double largestMotorVa, double totalMotorVa, List<MotorEntry> entries) {
	}

	MotorRows motorRows(GeneratorSetSizingInput input, double phaseFactor) {
		boolean threePhase = input.phaseType() == MotorPhaseType.THREE_PHASE;
		List<LoadRow> rows = new ArrayList<>();
		List<MotorEntry> entries = new ArrayList<>();
		double largestVa = 0;
		double totalVa = 0;
		for (MotorLoad motor : input.motors()) {
			double flcAmps = motorFlcCalculator.calculate(new MotorFlcInput(
					input.phaseType(), threePhase ? MotorClass.INDUCTION : null,
					motor.horsepowerLabel(), motor.voltage(), null)).flcAmps();
			double eachVa = phaseFactor * motor.voltage() * flcAmps;
			double groupVa = eachVa * motor.quantity();
			largestVa = Math.max(largestVa, eachVa);
			totalVa += groupVa;
			String defaultLabel = motor.horsepowerLabel() + " hp motor";
			String label = labelOf(motor.label(), defaultLabel);
			if (motor.quantity() > 1) {
				label = motor.quantity() + " x " + label;
			}
			String basis = String.format(Locale.ROOT,
					"Table %s: %s hp at %d V draws %s A, %s kVA each; 100%% at full-load current (2.20.3.11, 4.30.2.4).",
					threePhase ? "4.30.14.4" : "4.30.14.2", motor.horsepowerLabel(), motor.voltage(), num(flcAmps), num(eachVa / 1000.0));
			// Not "continuous": a motor's duty is not asked for, and the 4.30.2.4 feeder treats motors by
			// their own rule (125% of the largest) rather than the continuous-load split of the others.
			rows.add(new LoadRow(LoadKind.MOTOR, label, groupVa, 100.0, groupVa, false, basis));
			entries.add(new MotorEntry(motor, label, groupVa));
		}
		return new MotorRows(rows, largestVa, totalVa, entries);
	}

	/** Walks a stepped demand factor: each tier's percent applies to the VA between the previous bound and its own. */
	static double tierDemand(double totalVa, List<DemandTier> tiers) {
		double remaining = totalVa;
		double previousBound = 0;
		double demand = 0;
		for (DemandTier tier : tiers) {
			double bound = tier.upToVoltAmperes() == null ? Double.POSITIVE_INFINITY : tier.upToVoltAmperes();
			double portion = Math.min(remaining, bound - previousBound);
			demand += portion * tier.percent() / 100.0;
			remaining -= portion;
			previousBound = bound;
			if (remaining <= 0) {
				break;
			}
		}
		return demand;
	}

	static String describeTiers(List<DemandTier> tiers) {
		List<String> parts = new ArrayList<>();
		double previousBound = 0;
		for (int i = 0; i < tiers.size(); i++) {
			DemandTier tier = tiers.get(i);
			if (tier.upToVoltAmperes() == null) {
				parts.add(i == 0
						? num(tier.percent()) + "% of the total"
						: num(tier.percent()) + "% of the remainder over " + num(previousBound) + " VA");
			} else {
				parts.add(i == 0
						? num(tier.percent()) + "% of the first " + num(tier.upToVoltAmperes()) + " VA"
						: num(tier.percent()) + "% from " + num(previousBound) + " to " + num(tier.upToVoltAmperes()) + " VA");
				previousBound = tier.upToVoltAmperes();
			}
		}
		return String.join(", ", parts);
	}

}
