package com.renzoproject.calc.core.smokecontrol.naturalventing;

import com.renzoproject.calc.core.smokecontrol.naturalventing.NaturalVentingResult.TransientInterface;
import java.util.function.DoubleUnaryOperator;

/**
 * The time-stepping check of the smoke layer boundary while the fire grows. NOT part of NFPA 204,
 * which sizes the vents for equilibrium (9.2.2.3); the result is labelled supplementary.
 *
 * <p>A volume balance of the reservoir, stepped at one second: the smoke volume changes at
 * {@code (mp - mv) / rho_s}, with
 * <ul>
 *   <li>{@code mp} the plume mass flow of Equations 9.2.3.6 and 9.2.3.7 at the heat release rate of the
 *       end of the step and at the <em>design</em> layer boundary height (the boundary the vents are
 *       meant to hold), so the plume is the one the design calculation uses;</li>
 *   <li>{@code T = To + K Qc / (cp mp)} and {@code rho_s = rho_o To / T} (9.2.4.3);</li>
 *   <li>{@code mv} the vent flow of Equation 9.2.4.1 with the inlet term, at the layer depth below
 *       the vent centre of the moving boundary.</li>
 * </ul>
 * The smoke volume above a height is that of a gable (or flat) reservoir; the volume inside a roof
 * monitor is neglected, which is conservative. The method was set against the IN1GO supporting
 * calculations (Warehouse 1 and 2, ultrafast fire), which it reproduces to within a second.
 */
final class TransientInterfaceModel {

	/** Time stepped to when the boundary has not reached the design height, s. */
	static final double HORIZON_SECONDS = 3600.0;
	private static final double STEP_SECONDS = 1.0;
	private static final double GRAVITY = 9.81;
	private static final double TOLERANCE = 1e-9;

	private TransientInterfaceModel() {
	}

	/**
	 * The smoke reservoir under a gable roof, or a flat one when the eave is at the ridge.
	 *
	 * @param width        m
	 * @param length       m
	 * @param ridgeHeight  m above the floor
	 * @param eaveHeight   m above the floor; at most the ridge height
	 */
	record Reservoir(double width, double length, double ridgeHeight, double eaveHeight) {

		/** Height of the layer boundary above the floor that holds the given smoke volume, m. */
		double heightFor(double volume) {
			if (volume <= 0) {
				return ridgeHeight;
			}
			double plan = width * length;
			double rise = ridgeHeight - eaveHeight;
			double volumeAboveEave = plan * rise / 2;
			if (rise > 0 && volume <= volumeAboveEave) {
				return ridgeHeight - Math.sqrt(2 * rise * volume / plan);
			}
			return Math.max(0.0, eaveHeight - (volume - volumeAboveEave) / plan);
		}

	}

	/**
	 * @param reservoir               the curtained area
	 * @param ventAerodynamicArea     Cd,v Av of all the vents, m2
	 * @param inletAerodynamicArea    Cd,i Ai of the inlets, m2; null for no restriction by the inlet
	 * @param ventCentreHeight        m above the floor
	 * @param ambientK                To, K
	 * @param ambientDensity          rho_o, kg/m3
	 * @param specificHeat            cp, kJ/kg-K
	 * @param layerEnergyFraction     K
	 * @param designLayerHeight       the boundary height to hold, m above the floor
	 * @param layerBoundaryAboveFire  zs of the design, m, at which the plume equations are evaluated
	 * @param lineFireFactor          factor on the plume flow (9.2.3.5), 1 for none
	 * @param heatReleaseRate         Q(t), kW, t in seconds from ignition
	 * @param diameter                equivalent fire diameter D(Q), m
	 * @param endTime                 tr + td, s
	 */
	record Parameters(
			Reservoir reservoir,
			double ventAerodynamicArea,
			Double inletAerodynamicArea,
			double ventCentreHeight,
			double ambientK,
			double ambientDensity,
			double specificHeat,
			double layerEnergyFraction,
			double designLayerHeight,
			double layerBoundaryAboveFire,
			double lineFireFactor,
			DoubleUnaryOperator heatReleaseRate,
			DoubleUnaryOperator diameter,
			double endTime) {
	}

	private record Outcome(double heightAtEnd, Double timeToDesignHeight) {
	}

	static TransientInterface run(Parameters p) {
		Outcome vented = simulate(p, true);
		Outcome unvented = simulate(p, false);
		return new TransientInterface(p.endTime(), vented.heightAtEnd(), vented.timeToDesignHeight(), unvented.timeToDesignHeight(),
				Math.max(HORIZON_SECONDS, 2 * p.endTime()));
	}

	private static Outcome simulate(Parameters p, boolean vented) {
		double horizon = Math.max(HORIZON_SECONDS, 2 * p.endTime());
		double ridge = p.reservoir().ridgeHeight();
		double time = 0;
		double volume = 0;
		double height = ridge;
		Double heightAtEnd = null;
		Double reached = null;
		while (time < horizon - TOLERANCE) {
			double next = time + STEP_SECONDS;
			if (heightAtEnd == null && p.endTime() < next) {
				next = p.endTime();
			}
			double dt = next - time;
			double q = p.heatReleaseRate().applyAsDouble(next);
			if (q > 0) {
				double plumeFlow = PlumeEquations.plume(q, p.diameter().applyAsDouble(q), p.layerBoundaryAboveFire(),
						"time-stepping check").massFlow() * p.lineFireFactor();
				double convective = NaturalVentingCalculator.CONVECTIVE_FRACTION * q;
				double rise = p.layerEnergyFraction() * convective / (p.specificHeat() * plumeFlow);
				double temperature = p.ambientK() + rise;
				double layerDensity = p.ambientDensity() * p.ambientK() / temperature;
				double ventFlow = 0;
				if (vented) {
					double depth = Math.max(0.0, p.ventCentreHeight() - Math.min(height, ridge));
					double flowPerArea = Math.sqrt(2 * p.ambientDensity() * p.ambientDensity() * GRAVITY * depth)
							* Math.sqrt(p.ambientK() * (temperature - p.ambientK()) / (temperature * temperature));
					double restriction = 1.0;
					if (p.inletAerodynamicArea() != null) {
						double ratio = p.ventAerodynamicArea() / p.inletAerodynamicArea();
						restriction = Math.sqrt(1 + ratio * ratio * p.ambientK() / temperature);
					}
					ventFlow = p.ventAerodynamicArea() * flowPerArea / restriction;
				}
				volume = Math.max(0.0, volume + (plumeFlow - ventFlow) / layerDensity * dt);
				height = p.reservoir().heightFor(volume);
			}
			time = next;
			if (heightAtEnd == null && time >= p.endTime() - TOLERANCE) {
				heightAtEnd = height;
			}
			if (reached == null && height < p.designLayerHeight() - TOLERANCE) {
				reached = time;
			}
			if (reached != null && (heightAtEnd != null || !vented)) {
				break;
			}
		}
		return new Outcome(heightAtEnd == null ? height : heightAtEnd, reached);
	}

}
