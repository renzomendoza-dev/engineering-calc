package com.renzoproject.calc.core.smokecontrol.naturalventing;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.smokecontrol.naturalventing.NaturalVentingResult.FireCaseResult;
import com.renzoproject.calc.core.smokecontrol.naturalventing.NaturalVentingResult.Regime;
import com.renzoproject.calc.core.smokecontrol.naturalventing.NaturalVentingResult.ZoneResult;
import java.util.ArrayList;
import java.util.List;

/**
 * NFPA 204 (2021) natural smoke and heat venting by the hand-calculation method of Section 9.2:
 * design fire (Chapter 8), mass flow in the plume (9.2.3), vent flow with the inlet term
 * (9.2.4.1), and the vent and inlet areas from equating the two (9.2.5.3), with the dimension
 * and spacing rules of Chapters 5 to 7 reported as checks.
 *
 * <p>Deliberately separate from the NFPA 92 calculators in this package: the flame height, virtual
 * origin, layer temperature factor K and vent equation all differ.
 *
 * <p>Standard values taken from the printed pages: {@code Q = c (t/tg)^2} with c = 1055 (8.3.1,
 * 8.3.5; Annex A.8.3.2 and Annex D print 1000, so {@link NaturalVentingInput#growthConstant()} is
 * an input), {@code D = (4Q/(pi Q''))^0.5} (8.3.7), {@code Qc = 0.7 Q}, and K = 0.5 (9.2.4.4).
 * The Annex D sample problem is reproduced in the unit tests.
 */
public class NaturalVentingCalculator implements Calculator<NaturalVentingInput, NaturalVentingResult> {

	private static final double KELVIN_OFFSET = 273.15;
	private static final double GRAVITY = 9.81;
	private static final double CONVECTIVE_FRACTION = 0.7;
	private static final double STEFAN_BOLTZMANN_KW = 5.67e-11;
	private static final double INLET_VELOCITY_LIMIT = 1.0;
	private static final double FEASIBLE_FIRE_COEFFICIENT = 23_200.0;
	private static final double AISLE_COEFFICIENT = 0.042;

	@Override
	public NaturalVentingResult calculate(NaturalVentingInput input) {
		List<FireCaseResult> caseResults = new ArrayList<>();
		for (FireCase fireCase : input.fireCases()) {
			VentZone zone = input.zones().stream().filter(z -> z.name().equals(fireCase.zoneName())).findFirst().orElseThrow();
			caseResults.add(calculateCase(input, zone, fireCase));
		}
		List<ZoneResult> zoneResults = input.zones().stream().map(this::zoneChecks).toList();
		return new NaturalVentingResult(List.copyOf(caseResults), zoneResults);
	}

	private FireCaseResult calculateCase(NaturalVentingInput input, VentZone zone, FireCase fireCase) {
		double ambientK = input.ambientTemperature() + KELVIN_OFFSET;
		double ambientDensity = input.ambientDensity();
		List<VentingCheck> checks = new ArrayList<>();

		if (fireCase.smokeLayerHeight() > zone.ceilingHeight()) {
			throw new CalculationException("fire case " + fireCase.name() + ": smokeLayerHeight is above the ceiling of zone " + zone.name());
		}
		double zs = fireCase.smokeLayerHeight() - fireCase.fireBaseHeight();
		double layerDepth = zone.ventCentreHeight() - fireCase.smokeLayerHeight();
		if (layerDepth <= 0) {
			throw new CalculationException("fire case " + fireCase.name()
					+ ": the smoke layer boundary is at or above the middle of the vents, so the layer depth d is not positive");
		}

		// Chapter 8: design fire.
		double uncappedQ;
		Double growthCoefficient = null;
		if (fireCase.kind() == FireCaseKind.STEADY) {
			uncappedQ = fireCase.steadyHeatReleaseRate();
		} else {
			double ratio = (fireCase.designInterval() + fireCase.detectionTime()) / fireCase.growthTime();
			uncappedQ = input.growthConstant() * ratio * ratio;
			growthCoefficient = input.growthConstant() / (fireCase.growthTime() * fireCase.growthTime());
		}
		boolean capped = fireCase.maxHeatReleaseRate() != null && uncappedQ > fireCase.maxHeatReleaseRate();
		double q = capped ? fireCase.maxHeatReleaseRate() : uncappedQ;
		double qc = CONVECTIVE_FRACTION * q;
		double diameter = fireDiameter(fireCase, q);

		if (fireCase.kind() == FireCaseKind.GROWING) {
			double atInterval = fireCase.designInterval() + fireCase.detectionTime();
			checks.add(new VentingCheck("8.3.5", "Design fire at the end of the design interval, Q = c((tr + td)/tg)^2 with c = "
					+ trim(input.growthConstant()), CheckBasis.NFPA_204, CheckStatus.INFO, uncappedQ, null, "kW",
					"Evaluated at tr + td = " + trim(atInterval) + " s; tr is measured from detection (3.3.8)."));
		}
		if (capped) {
			checks.add(new VentingCheck("8.1.1", "Growing fire limited to its maximum heat release rate", CheckBasis.NFPA_204,
					CheckStatus.CONFIRM, q, uncappedQ, "kW",
					"The fuel available must really limit the fire to this size; confirm with the fuel load."));
		}

		// 9.2.3: plume.
		double flameHeight = -1.02 * diameter + 0.235 * Math.pow(q, 2.0 / 5.0);
		double virtualOrigin = 0.083 * Math.pow(q, 2.0 / 5.0) - 1.02 * diameter;
		Regime regime = flameHeight < zs ? Regime.BELOW_LAYER : Regime.REACHES_LAYER;
		double plumeFlow;
		if (regime == Regime.BELOW_LAYER) {
			double height = zs - virtualOrigin;
			if (height <= 0) {
				throw new CalculationException("fire case " + fireCase.name()
						+ ": the smoke layer boundary is at or below the virtual origin of the plume, so Equation 9.2.3.6 does not apply");
			}
			plumeFlow = 0.071 * Math.cbrt(qc) * Math.pow(height, 5.0 / 3.0)
					* (1 + 0.027 * Math.pow(qc, 2.0 / 3.0) * Math.pow(height, -5.0 / 3.0));
		} else {
			plumeFlow = 0.0056 * qc * zs / flameHeight;
		}
		double lineFireFactor = 1.0;
		if (fireCase.largestFireDimension() != null) {
			double wf = fireCase.largestFireDimension();
			if (zs < 4 * wf) {
				lineFireFactor = Math.pow(4 * wf / zs, 2.0 / 3.0);
				plumeFlow *= lineFireFactor;
				checks.add(new VentingCheck("9.2.3.5", "Line-like fire: layer boundary below four times the largest fire dimension, so the "
						+ "smoke production is increased by [4Wf/zs]^(2/3)", CheckBasis.NFPA_204, CheckStatus.INFO, zs, 4 * wf, "m",
						"Factor applied: " + trim(lineFireFactor) + "."));
			} else {
				checks.add(new VentingCheck("9.2.3.4", "Line-like fire: layer boundary at least four times the largest fire dimension",
						CheckBasis.NFPA_204, CheckStatus.PASS, zs, 4 * wf, "m", null));
			}
		}

		// 9.2.4.3 and 9.2.4.4: smoke layer temperature.
		double temperatureRise = (input.layerEnergyFraction() * qc) / (input.specificHeat() * plumeFlow);
		double layerTemperature = ambientK + temperatureRise;
		double layerDensity = ambientDensity * ambientK / layerTemperature;
		if (input.layerEnergyFraction() != NaturalVentingInput.DEFAULT_LAYER_ENERGY_FRACTION) {
			checks.add(new VentingCheck("9.2.4.4", "K other than 0.5 needs an analysis acceptable to the AHJ", CheckBasis.NFPA_204,
					CheckStatus.CONFIRM, input.layerEnergyFraction(), NaturalVentingInput.DEFAULT_LAYER_ENERGY_FRACTION, null, null));
		}

		// 9.2.4.1: vent flow per unit of Cd,v Av, with no inlet restriction.
		double ventFlowFactor = Math.sqrt(2 * ambientDensity * ambientDensity * GRAVITY * layerDepth)
				* Math.sqrt(ambientK * (layerTemperature - ambientK) / (layerTemperature * layerTemperature));
		double aerodynamicArea = plumeFlow / ventFlowFactor;
		double temperatureRatio = ambientK / layerTemperature;
		double cdVent = zone.ventDischargeCoefficient();
		double cdInlet = zone.inletDischargeCoefficient();

		// 9.2.5.3: required vent area with the inlet term.
		Double requiredVentArea = null;
		Double inletCorrection = null;
		Double requiredInletArea = null;
		Double inletArea = zone.inletNetArea();
		if (zone.inletNetArea() != null) {
			double cdInletArea = cdInlet * zone.inletNetArea();
			double restriction = 1 - Math.pow(aerodynamicArea / cdInletArea, 2) * temperatureRatio;
			if (restriction > 0) {
				inletCorrection = 1 / Math.sqrt(restriction);
				requiredVentArea = aerodynamicArea / cdVent * inletCorrection;
				checks.add(new VentingCheck("9.2.5.2", "Inlet area provided passes the plume flow", CheckBasis.NFPA_204, CheckStatus.PASS,
						zone.inletNetArea(), null, "m2", "Inlet term M = " + trim(inletCorrection) + "."));
			} else {
				checks.add(new VentingCheck("9.2.5.2", "Inlet area provided cannot pass the plume flow at this layer temperature, "
						+ "so no vent area is large enough", CheckBasis.NFPA_204, CheckStatus.FAIL, cdInletArea,
						aerodynamicArea * Math.sqrt(temperatureRatio), "m2",
						"Cd,i x Ai must exceed Aa x (To/T)^0.5. Enlarge the inlets."));
			}
		} else if (zone.inletAreaRatio() != null) {
			double areaRatio = cdVent / cdInlet / zone.inletAreaRatio();
			inletCorrection = Math.sqrt(1 + areaRatio * areaRatio * temperatureRatio);
			requiredVentArea = aerodynamicArea / cdVent * inletCorrection;
			requiredInletArea = zone.inletAreaRatio() * requiredVentArea;
			inletArea = requiredInletArea;
			checks.add(new VentingCheck("9.2.5.2", "Inlet area taken as " + trim(zone.inletAreaRatio()) + " times the vent area",
					CheckBasis.NFPA_204, CheckStatus.INFO, requiredInletArea, null, "m2",
					"Inlet term M = " + trim(inletCorrection) + ". Provide at least this inlet area."));
		} else {
			requiredVentArea = aerodynamicArea / cdVent;
			checks.add(new VentingCheck("9.2.5.2", "No inlet area or inlet ratio given: the required vent area ignores the inlet term",
					CheckBasis.NFPA_204, CheckStatus.OPEN, null, null, "m2",
					"The inlet area must be sized with the vents (9.2.5.2); the vent area below is a lower bound."));
		}

		// Vent area provided against required.
		if (zone.ventTotalArea() != null && requiredVentArea != null) {
			checks.add(new VentingCheck("9.2.5.1", "Total geometric vent area provided is at least the area required to hold the layer",
					CheckBasis.NFPA_204, zone.ventTotalArea() >= requiredVentArea ? CheckStatus.PASS : CheckStatus.FAIL,
					zone.ventTotalArea(), requiredVentArea, "m2", null));
		} else if (requiredVentArea != null) {
			checks.add(new VentingCheck("9.2.5.1", "Required total geometric vent area", CheckBasis.NFPA_204, CheckStatus.INFO,
					null, requiredVentArea, "m2", "No vent area provided to compare with."));
		}

		// 6.6.3: air velocity.
		double inletForVelocityLimit = plumeFlow / (ambientDensity * INLET_VELOCITY_LIMIT);
		Double inletVelocity = inletArea == null ? null : plumeFlow / (ambientDensity * inletArea);
		if (inletVelocity != null) {
			checks.add(new VentingCheck("6.6.3", "Air velocity through the inlets not above 1 m/s", CheckBasis.NFPA_204,
					inletVelocity <= INLET_VELOCITY_LIMIT ? CheckStatus.PASS : CheckStatus.FAIL, inletVelocity, INLET_VELOCITY_LIMIT, "m/s",
					"The standard limits the velocity at the plume; applying it at the inlet face is conservative."));
		}

		// 5.4.1: unit vent dimensions.
		Double unitArea = unitVentArea(zone);
		if (unitArea != null) {
			double limit = 2 * layerDepth * layerDepth;
			checks.add(new VentingCheck("5.4.1.1", "Area of one vent not above 2d^2", CheckBasis.NFPA_204,
					unitArea <= limit ? CheckStatus.PASS : CheckStatus.FAIL, unitArea, limit, "m2", "d = " + trim(layerDepth) + " m."));
		} else {
			checks.add(new VentingCheck("5.4.1.1", "Area of one vent not above 2d^2", CheckBasis.NFPA_204, CheckStatus.OPEN,
					null, 2 * layerDepth * layerDepth, "m2", "Give the vent size, or the vent count and total area."));
		}
		if (zone.ventUnitLength() != null && zone.ventUnitWidth() != null) {
			double length = Math.max(zone.ventUnitLength(), zone.ventUnitWidth());
			double width = Math.min(zone.ventUnitLength(), zone.ventUnitWidth());
			if (length / width > 2) {
				checks.add(new VentingCheck("5.4.1.2", "A vent with length over twice its width: width not above d", CheckBasis.NFPA_204,
						width <= layerDepth ? CheckStatus.PASS : CheckStatus.FAIL, width, layerDepth, "m", null));
			}
		}

		// 9.2.2.2: boundary at or above the bottom of the curtains.
		if (zone.curtainDepth() != null) {
			double curtainBottom = zone.ceilingHeight() - zone.curtainDepth();
			checks.add(new VentingCheck("9.2.2.2", "Smoke layer boundary at or above the bottom of the draft curtains", CheckBasis.NFPA_204,
					fireCase.smokeLayerHeight() >= curtainBottom ? CheckStatus.PASS : CheckStatus.FAIL,
					fireCase.smokeLayerHeight(), curtainBottom, "m", null));
		}

		// 4.1: layer temperature objective, and the Annex D floor heat flux.
		if (zone.maxLayerTemperatureRise() != null) {
			checks.add(new VentingCheck("4.1", "Smoke layer temperature rise within the design objective", CheckBasis.NFPA_204,
					temperatureRise <= zone.maxLayerTemperatureRise() ? CheckStatus.PASS : CheckStatus.FAIL,
					temperatureRise, zone.maxLayerTemperatureRise(), "K", null));
		} else {
			checks.add(new VentingCheck("4.1", "No maximum allowable layer temperature given; the design objectives must include one",
					CheckBasis.NFPA_204, CheckStatus.OPEN, temperatureRise, null, "K", null));
		}
		double floorFlux = STEFAN_BOLTZMANN_KW * Math.pow(layerTemperature, 4);
		if (zone.maxFloorRadiantFlux() != null) {
			checks.add(new VentingCheck("Annex D.5", "Radiant heat flux from the layer to the floor within the objective",
					CheckBasis.SUPPLEMENTARY, floorFlux <= zone.maxFloorRadiantFlux() ? CheckStatus.PASS : CheckStatus.FAIL,
					floorFlux, zone.maxFloorRadiantFlux(), "kW/m2", "sigma T^4 with emissivity and configuration factor 1."));
		}
		double feasibleQ = FEASIBLE_FIRE_COEFFICIENT * Math.pow(zs, 2.5);
		checks.add(new VentingCheck("Annex D.7", "Design fire not above the feasible fire for this layer height, 23,200 zs^(5/2)",
				CheckBasis.SUPPLEMENTARY, q <= feasibleQ ? CheckStatus.PASS : CheckStatus.CONFIRM, q, feasibleQ, "kW", null));

		Double minimumAisle = null;
		if (fireCase.kind() == FireCaseKind.STEADY) {
			minimumAisle = AISLE_COEFFICIENT * Math.sqrt(q);
			if (zone.aisleWidth() != null) {
				checks.add(new VentingCheck("8.2.3", "Aisle width at least 0.042 Q^(1/2) to stop lateral fire spread by radiation",
						CheckBasis.NFPA_204, zone.aisleWidth() >= minimumAisle ? CheckStatus.PASS : CheckStatus.FAIL,
						zone.aisleWidth(), minimumAisle, "m", null));
			}
		}

		return new FireCaseResult(fireCase.name(), zone.name(), q, capped, growthCoefficient, diameter, flameHeight, virtualOrigin, zs,
				layerDepth, regime, lineFireFactor, qc, plumeFlow, temperatureRise, layerTemperature, layerDensity, aerodynamicArea,
				requiredVentArea, inletCorrection, requiredInletArea, inletForVelocityLimit, inletVelocity, floorFlux, feasibleQ,
				minimumAisle, List.copyOf(checks));
	}

	private ZoneResult zoneChecks(VentZone zone) {
		List<VentingCheck> checks = new ArrayList<>();
		double h = zone.ceilingHeight();

		if (zone.curtainDepth() != null) {
			checks.add(new VentingCheck("7.3.1", "Draft curtain depth at least 20 percent of the ceiling height", CheckBasis.NFPA_204,
					zone.curtainDepth() >= 0.2 * h ? CheckStatus.PASS : CheckStatus.FAIL, zone.curtainDepth(), 0.2 * h, "m", null));
		} else {
			checks.add(new VentingCheck("7.3.1", "Draft curtain depth at least 20 percent of the ceiling height", CheckBasis.NFPA_204,
					CheckStatus.OPEN, null, 0.2 * h, "m", "Give the curtain depth."));
		}
		if (zone.zoneLength() != null && zone.zoneWidth() != null) {
			double longest = Math.max(zone.zoneLength(), zone.zoneWidth());
			checks.add(new VentingCheck("7.4.1", "Neither length nor width of the curtained area above eight times the ceiling height",
					CheckBasis.NFPA_204, longest <= 8 * h ? CheckStatus.PASS : CheckStatus.FAIL, longest, 8 * h, "m", null));
			if (zone.curtainDepth() != null && zone.curtainDepth() < 0.3 * h) {
				double shortest = Math.min(zone.zoneLength(), zone.zoneWidth());
				checks.add(new VentingCheck("7.4.2", "Curtains less than 30 percent of the ceiling height deep: spacing not below the "
						+ "ceiling height", CheckBasis.NFPA_204, shortest >= h ? CheckStatus.PASS : CheckStatus.FAIL, shortest, h, "m", null));
			}
		} else {
			checks.add(new VentingCheck("7.4.1", "Neither length nor width of the curtained area above eight times the ceiling height",
					CheckBasis.NFPA_204, CheckStatus.OPEN, null, 8 * h, "m", "Give the length and width of the curtained area."));
		}
		if (zone.ventSpacing() != null) {
			checks.add(new VentingCheck("5.4.2", "Vent centre-to-centre spacing not above 4H", CheckBasis.NFPA_204,
					zone.ventSpacing() <= 4 * h ? CheckStatus.PASS : CheckStatus.FAIL, zone.ventSpacing(), 4 * h, "m", null));
		} else {
			checks.add(new VentingCheck("5.4.2", "Vent centre-to-centre spacing not above 4H", CheckBasis.NFPA_204,
					CheckStatus.OPEN, null, 4 * h, "m", "Give the vent spacing."));
		}
		if (zone.ventToWallDistance() != null) {
			checks.add(new VentingCheck("5.4.3", "Horizontal distance from a wall or curtain to the nearest vent not above 2.8H",
					CheckBasis.NFPA_204, zone.ventToWallDistance() <= 2.8 * h ? CheckStatus.PASS : CheckStatus.FAIL,
					zone.ventToWallDistance(), 2.8 * h, "m", null));
		} else {
			checks.add(new VentingCheck("5.4.3", "Horizontal distance from a wall or curtain to the nearest vent not above 2.8H",
					CheckBasis.NFPA_204, CheckStatus.OPEN, null, 2.8 * h, "m", "Give the distance from the walls and curtains to the nearest vent."));
		}
		return new ZoneResult(zone.name(), List.copyOf(checks));
	}

	private static double fireDiameter(FireCase fireCase, double q) {
		if (fireCase.fireDiameter() != null) {
			return fireCase.fireDiameter();
		}
		if (fireCase.fireArea() != null) {
			return Math.sqrt(4 * fireCase.fireArea() / Math.PI);
		}
		return Math.sqrt(4 * q / (Math.PI * fireCase.unitHeatReleaseRate()));
	}

	private static Double unitVentArea(VentZone zone) {
		if (zone.ventUnitLength() != null && zone.ventUnitWidth() != null) {
			return zone.ventUnitLength() * zone.ventUnitWidth();
		}
		if (zone.ventTotalArea() != null && zone.ventCount() != null) {
			return zone.ventTotalArea() / zone.ventCount();
		}
		return null;
	}

	private static String trim(double value) {
		return value == Math.rint(value) ? String.valueOf((long) value) : String.format("%.3g", value);
	}

}
