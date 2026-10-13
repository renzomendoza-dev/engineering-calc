package com.renzoproject.calc.core.mechanical.sprinkler;

/**
 * What one listed sprinkler discharges, and at what pressure. <b>Units:</b> psi, gpm.
 *
 * <p>The segment rows carry the flow through each pipe, which accumulates along a branch; this is
 * the flow of each sprinkler on its own, which is what to read when asking whether a sprinkler is
 * getting what the design density asks of it.
 *
 * <p>Only the most remote sprinkler is held at the minimum. Every other one discharges at the
 * pressure the network gives it ({@code Q = K * sqrt(P)}), and a sprinkler on a branch that was
 * balanced at a junction discharges at the scaled flow -- {@code pressurePsi} is then the pressure
 * that flow corresponds to, {@code (Q / K)^2}.
 *
 * @param nodeId         the sprinkler's node
 * @param kFactor        its K factor
 * @param flowGpm        what it discharges, after any junction balancing
 * @param pressurePsi    the pressure at that flow
 * @param minimumFlowGpm what the design density asks of it: {@code density * coverage area}. A
 *                       sprinkler is never solved below this
 */
public record SprinklerFlowRow(
		String nodeId,
		double kFactor,
		double flowGpm,
		double pressurePsi,
		double minimumFlowGpm) {

}
