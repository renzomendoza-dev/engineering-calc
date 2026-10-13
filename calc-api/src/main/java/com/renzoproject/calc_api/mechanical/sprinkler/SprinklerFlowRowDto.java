package com.renzoproject.calc_api.mechanical.sprinkler;

import com.renzoproject.calc.core.mechanical.sprinkler.SprinklerFlowRow;

/**
 * What one listed sprinkler discharges and at what pressure, mirroring calc-core's
 * {@link SprinklerFlowRow}. <b>Units:</b> psi, gpm.
 *
 * <p>The segment rows carry flow that accumulates along a branch; this is each sprinkler on its own.
 * Only the most remote sprinkler is held at the minimum -- the rest discharge at the pressure the
 * network gives them -- so the figures here are what to read when asking what a sprinkler is
 * actually delivering.
 *
 * @param nodeId         the sprinkler's node
 * @param kFactor        its K factor
 * @param flowGpm        what it discharges, after any junction balancing
 * @param pressurePsi    the pressure at that flow
 * @param minimumFlowGpm what the design density asks of it: density x coverage area
 */
public record SprinklerFlowRowDto(
		String nodeId,
		Double kFactor,
		Double flowGpm,
		Double pressurePsi,
		Double minimumFlowGpm) {

	public static SprinklerFlowRowDto from(SprinklerFlowRow row) {
		return new SprinklerFlowRowDto(
				row.nodeId(), row.kFactor(), row.flowGpm(), row.pressurePsi(), row.minimumFlowGpm());
	}

}
