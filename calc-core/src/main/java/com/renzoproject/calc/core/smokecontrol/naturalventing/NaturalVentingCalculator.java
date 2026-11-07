package com.renzoproject.calc.core.smokecontrol.naturalventing;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.smokecontrol.naturalventing.NaturalVentingResult.FireCaseResult;
import com.renzoproject.calc.core.smokecontrol.naturalventing.NaturalVentingResult.ImageFire;
import com.renzoproject.calc.core.smokecontrol.naturalventing.NaturalVentingResult.ImageFireScenario;
import com.renzoproject.calc.core.smokecontrol.naturalventing.NaturalVentingResult.Regime;
import com.renzoproject.calc.core.smokecontrol.naturalventing.NaturalVentingResult.TransientInterface;
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
	static final double CONVECTIVE_FRACTION = 0.7;
	private static final double STEFAN_BOLTZMANN_KW = 5.67e-11;
	private static final double INLET_VELOCITY_LIMIT = 1.0;
	/** Equation 1.3.5: the standard is not valid for a fire above 12,000 zs^(5/2) kW. */
	private static final double FEASIBLE_FIRE_COEFFICIENT = 12_000.0;
	/** The coefficient the Annex D sample problem prints for the same check (D.7h); the standard's own equation 1.3.5 prints 12,000. */
	private static final double ANNEX_D_FEASIBLE_FIRE_COEFFICIENT = 23_200.0;
	/** 1.3.4: a vent design is not valid while the smoke layer is above 600 C. */
	private static final double MAXIMUM_LAYER_TEMPERATURE_C = 600.0;
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
		Double detectionTime = null;
		boolean detectionFromDetector = false;
		if (fireCase.kind() == FireCaseKind.STEADY) {
			uncappedQ = fireCase.steadyHeatReleaseRate();
		} else {
			detectionTime = fireCase.detectionTime();
			if (fireCase.detectorDistance() != null) {
				detectionTime = smokeDetectorDetectionTime(zone, fireCase, checks);
				detectionFromDetector = true;
			}
			double ratio = (fireCase.designInterval() + detectionTime) / fireCase.growthTime();
			uncappedQ = input.growthConstant() * ratio * ratio;
			growthCoefficient = input.growthConstant() / (fireCase.growthTime() * fireCase.growthTime());
		}
		boolean capped = fireCase.maxHeatReleaseRate() != null && uncappedQ > fireCase.maxHeatReleaseRate();
		double q = capped ? fireCase.maxHeatReleaseRate() : uncappedQ;
		double qc = CONVECTIVE_FRACTION * q;
		double diameter = fireDiameter(fireCase, q);

		if (fireCase.kind() == FireCaseKind.GROWING) {
			double atInterval = fireCase.designInterval() + detectionTime;
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
		PlumeEquations.Plume designPlume = PlumeEquations.plume(q, diameter, zs, "fire case " + fireCase.name());
		double flameHeight = designPlume.flameHeight();
		double virtualOrigin = designPlume.virtualOrigin();
		Regime regime = designPlume.regime();
		double plumeFlow = designPlume.massFlow();
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

		// 1.3.4: the standard is not valid while the smoke layer is above 600 C.
		double maximumLayerK = MAXIMUM_LAYER_TEMPERATURE_C + KELVIN_OFFSET;
		checks.add(new VentingCheck("1.3.4", "Smoke layer temperature not above 600 C, above which a vent design is not valid", CheckBasis.NFPA_204,
				layerTemperature <= maximumLayerK ? CheckStatus.PASS : CheckStatus.FAIL, layerTemperature, maximumLayerK, "K",
				"Above 600 C the radiation from the layer can ignite the combustibles under the curtained area (A.1.3.4)."));

		// 1.3.5: the standard is not valid for a fire above Qfeasible. Annex D prints a different coefficient for the same check.
		double feasibleQ = FEASIBLE_FIRE_COEFFICIENT * Math.pow(zs, 2.5);
		checks.add(new VentingCheck("1.3.5", "Design fire not above the feasible fire for this layer height, 12,000 zs^(5/2), above which the "
				+ "standard is not valid", CheckBasis.NFPA_204, q <= feasibleQ ? CheckStatus.PASS : CheckStatus.FAIL, q, feasibleQ, "kW",
				"Venting a larger fire needs more vent area than this calculation gives, and a fire near ventilation control may not support a "
						+ "clear layer (A.1.3.5)."));
		double annexDFeasibleQ = ANNEX_D_FEASIBLE_FIRE_COEFFICIENT * Math.pow(zs, 2.5);
		checks.add(new VentingCheck("Annex D.7h", "Annex D checks the same limit with 23,200 zs^(5/2); equation 1.3.5 prints 12,000, which governs",
				CheckBasis.SUPPLEMENTARY, CheckStatus.INFO, q, annexDFeasibleQ, "kW", null));

		// Supplementary, not NFPA 204: a fire against a wall or in a corner, and the layer boundary while the fire grows.
		ImageFire imageFire = imageFire(input, zone, fireCase, q, qc, diameter, zs, checks);
		TransientInterface transientInterface = null;
		if (fireCase.kind() == FireCaseKind.GROWING) {
			transientInterface = transientInterface(input, zone, fireCase, ambientK, ambientDensity, lineFireFactor, zs, detectionTime, checks);
		}

		Double minimumAisle = null;
		if (fireCase.kind() == FireCaseKind.STEADY) {
			minimumAisle = AISLE_COEFFICIENT * Math.sqrt(q);
			if (zone.aisleWidth() != null) {
				checks.add(new VentingCheck("8.2.3", "Aisle width at least 0.042 Q^(1/2) to stop lateral fire spread by radiation",
						CheckBasis.NFPA_204, zone.aisleWidth() >= minimumAisle ? CheckStatus.PASS : CheckStatus.FAIL,
						zone.aisleWidth(), minimumAisle, "m", null));
			}
		}

		return new FireCaseResult(fireCase.name(), zone.name(), q, capped, growthCoefficient, detectionTime, detectionFromDetector,
				diameter, flameHeight, virtualOrigin, zs,
				layerDepth, regime, lineFireFactor, qc, plumeFlow, temperatureRise, layerTemperature, layerDensity, aerodynamicArea,
				requiredVentArea, inletCorrection, requiredInletArea, inletForVelocityLimit, inletVelocity, floorFlux, feasibleQ,
				minimumAisle, List.copyOf(checks), imageFire, transientInterface);
	}

	/**
	 * The smoke layer of a fire against a wall (2) or in a corner (4) by the image-fire method of the smoke
	 * management literature (Klote and Milke; the SFPE Handbook): the fire is treated as {@code n} times its size
	 * in open space and the plume flow divided by {@code n}, so {@code dT = K Qc / (cp mp / n)}. NFPA 204 4.5.3.1
	 * applies the entrainment equations to a single fire only, so this is a screening estimate, labelled
	 * supplementary. The line-fire factor is not applied, which keeps the temperature on the high side.
	 */
	private ImageFire imageFire(NaturalVentingInput input, VentZone zone, FireCase fireCase, double q, double qc, double diameter, double zs,
			List<VentingCheck> checks) {
		double ambientK = input.ambientTemperature() + KELVIN_OFFSET;
		ImageFireScenario wall = imageScenario(input, fireCase, 2, q, qc, diameter, zs, ambientK);
		ImageFireScenario corner = imageScenario(input, fireCase, 4, q, qc, diameter, zs, ambientK);
		addImageFireCheck(zone, "Image fire (wall)", "Fire against a wall", wall, input, checks);
		addImageFireCheck(zone, "Image fire (corner)", "Fire in a corner", corner, input, checks);
		return new ImageFire(wall, corner);
	}

	private ImageFireScenario imageScenario(NaturalVentingInput input, FireCase fireCase, int multiplier, double q, double qc, double diameter,
			double zs, double ambientK) {
		double equivalentQ = multiplier * q;
		PlumeEquations.Plume plume = PlumeEquations.plume(equivalentQ, diameter * Math.sqrt(multiplier), zs,
				"fire case " + fireCase.name() + " as an image fire");
		double plumeFlow = plume.massFlow() / multiplier;
		double rise = (input.layerEnergyFraction() * qc) / (input.specificHeat() * plumeFlow);
		return new ImageFireScenario(multiplier, equivalentQ, plumeFlow, rise, ambientK + rise);
	}

	private void addImageFireCheck(VentZone zone, String clause, String what, ImageFireScenario scenario, NaturalVentingInput input,
			List<VentingCheck> checks) {
		String note = "Treated as " + trim(scenario.multiplier()) + " x Q in open space with the plume flow divided by " + trim(scenario.multiplier())
				+ ", K = " + trim(input.layerEnergyFraction()) + ", no line-fire factor. A screening estimate from the smoke management "
				+ "literature, not NFPA 204, which applies the entrainment equations to a single fire (4.5.3.1).";
		if (zone.maxLayerTemperatureRise() != null) {
			checks.add(new VentingCheck(clause, what + ": smoke layer temperature rise within the design objective", CheckBasis.SUPPLEMENTARY,
					scenario.layerTemperatureRise() <= zone.maxLayerTemperatureRise() ? CheckStatus.PASS : CheckStatus.FAIL,
					scenario.layerTemperatureRise(), zone.maxLayerTemperatureRise(), "K", note));
		} else {
			checks.add(new VentingCheck(clause, what + ": smoke layer temperature rise", CheckBasis.SUPPLEMENTARY, CheckStatus.INFO,
					scenario.layerTemperatureRise(), null, "K", note));
		}
	}

	/**
	 * The supplementary time-stepping check (see {@link TransientInterfaceModel}). Needs the zone's length, width
	 * and total vent area: without them the check is reported open.
	 */
	private TransientInterface transientInterface(NaturalVentingInput input, VentZone zone, FireCase fireCase, double ambientK,
			double ambientDensity, double lineFireFactor, double zs, double detectionTime, List<VentingCheck> checks) {
		if (zone.zoneLength() == null || zone.zoneWidth() == null || zone.ventTotalArea() == null) {
			checks.add(new VentingCheck("Time-stepping", "Smoke layer boundary at the end of the design interval, stepped as the fire grows",
					CheckBasis.SUPPLEMENTARY, CheckStatus.OPEN, null, null, "m",
					"Give the length and width of the curtained area and the total vent area to run it. Not NFPA 204, which sizes the vents for equilibrium (9.2.2.3)."));
			return null;
		}
		double ventAerodynamic = zone.ventDischargeCoefficient() * zone.ventTotalArea();
		Double inletAerodynamic = null;
		if (zone.inletNetArea() != null) {
			inletAerodynamic = zone.inletDischargeCoefficient() * zone.inletNetArea();
		} else if (zone.inletAreaRatio() != null) {
			inletAerodynamic = zone.inletDischargeCoefficient() * zone.inletAreaRatio() * zone.ventTotalArea();
		}
		double eave = zone.eaveHeight() != null ? zone.eaveHeight() : zone.ceilingHeight();
		double growth = input.growthConstant() / (fireCase.growthTime() * fireCase.growthTime());
		Double cap = fireCase.maxHeatReleaseRate();
		double endTime = fireCase.designInterval() + detectionTime;
		TransientInterfaceModel.Parameters parameters = new TransientInterfaceModel.Parameters(
				new TransientInterfaceModel.Reservoir(zone.zoneWidth(), zone.zoneLength(), zone.ceilingHeight(), eave),
				ventAerodynamic, inletAerodynamic, zone.ventCentreHeight(), ambientK, ambientDensity, input.specificHeat(),
				input.layerEnergyFraction(), fireCase.smokeLayerHeight(), zs, lineFireFactor,
				t -> cap == null ? growth * t * t : Math.min(growth * t * t, cap),
				heatReleaseRate -> fireDiameter(fireCase, heatReleaseRate), endTime);
		TransientInterface result = TransientInterfaceModel.run(parameters);

		String basis = "Volume balance stepped at 1 s with the plume of 9.2.3.6/9.2.3.7 at the design layer height, K = "
				+ trim(input.layerEnergyFraction()) + ", the vent flow of 9.2.4.1 with the inlet term, and the volume of a "
				+ (zone.eaveHeight() == null ? "flat" : "gable") + " reservoir (the volume inside a roof monitor is neglected). "
				+ "Not NFPA 204, which sizes the vents for equilibrium (9.2.2.3); a CFD or other analysis the AHJ accepts governs.";
		checks.add(new VentingCheck("Time-stepping", "Smoke layer boundary at or above the design height at the end of the design interval, stepped as the fire grows",
				CheckBasis.SUPPLEMENTARY, result.interfaceHeightAtEnd() >= fireCase.smokeLayerHeight() - 1e-9 ? CheckStatus.PASS : CheckStatus.FAIL,
				result.interfaceHeightAtEnd(), fireCase.smokeLayerHeight(), "m", "Read at tr + td = " + trim(endTime) + " s. " + basis));
		checks.add(new VentingCheck("Time-stepping", "Time for the boundary to fall below the design height with the vents working, if the fire kept growing",
				CheckBasis.SUPPLEMENTARY, CheckStatus.INFO, result.timeToDesignHeight(), null, "s",
				result.timeToDesignHeight() == null ? "Not reached within " + trim(result.horizon() / 60) + " minutes." : "From ignition."));
		checks.add(new VentingCheck("Time-stepping", "Time for the boundary to fall below the design height with no venting",
				CheckBasis.SUPPLEMENTARY, CheckStatus.INFO, result.timeToDesignHeightWithoutVents(), null, "s",
				result.timeToDesignHeightWithoutVents() == null ? "Not reached within " + trim(result.horizon() / 60) + " minutes." : "From ignition."));
		return result;
	}

	/**
	 * td of the smoke detector that controls detection, from the time its location reaches the
	 * activation temperature rise (9.2.5.4.3). H is the ceiling height above the base of the fire.
	 */
	private double smokeDetectorDetectionTime(VentZone zone, FireCase fireCase, List<VentingCheck> checks) {
		double height = zone.ceilingHeight() - fireCase.fireBaseHeight();
		boolean minimum = fireCase.detectorTemperatureRise() == null;
		double rise = minimum ? SmokeDetectorDetectionTime.MINIMUM_TEMPERATURE_RISE : fireCase.detectorTemperatureRise();
		double td = SmokeDetectorDetectionTime.timeToTemperatureRise(fireCase.growthTime(), height, fireCase.detectorDistance(), rise);
		checks.add(new VentingCheck("9.2.5.4.3", "Detection time of the smoke detector, from the gas temperature rise at its location",
				CheckBasis.NFPA_204, CheckStatus.INFO, td, null, "s",
				"Rise of " + trim(rise) + " C at " + trim(fireCase.detectorDistance()) + " m from the fire axis, H = " + trim(height)
						+ " m, tg = " + trim(fireCase.growthTime()) + " s. The fire is taken at the farthest distance from the detector that "
						+ "controls detection (9.2.5.4.2). From the printed equation, not DETACT-QS."));
		if (minimum) {
			checks.add(new VentingCheck("9.2.5.4.3.2", "Temperature rise for activation taken as the 20 C minimum", CheckBasis.NFPA_204,
					CheckStatus.INFO, rise, SmokeDetectorDetectionTime.MINIMUM_TEMPERATURE_RISE, "C",
					"Used because no test data are given for the detector and the fuel (9.2.5.4.3.1)."));
		} else if (rise < SmokeDetectorDetectionTime.MINIMUM_TEMPERATURE_RISE) {
			checks.add(new VentingCheck("9.2.5.4.3.1", "Temperature rise below the 20 C minimum needs dedicated test data", CheckBasis.NFPA_204,
					CheckStatus.CONFIRM, rise, SmokeDetectorDetectionTime.MINIMUM_TEMPERATURE_RISE, "C",
					"The rise must come from tests, or the equivalent, for the combustibles of the occupancy and the detector model to be installed."));
		}
		return td;
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
