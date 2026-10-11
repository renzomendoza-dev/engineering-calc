package com.renzoproject.calc_api.mechanical.sprinkler;

/**
 * What kind of source the water comes from, which decides which block of
 * {@link WaterSupplyDto} is filled in.
 *
 * <p>There is no {@code NONE} here on purpose: a system with no pressurised source omits
 * {@code waterSupply} altogether, so "no supply" has one representation rather than two.
 */
public enum WaterSupplySourceDto {

	/** A city main, or anything else described by a flow test. */
	FLOW_TEST,

	/** An elevated tank, described by its height above the connection and the run down to it. */
	GRAVITY_TANK
}
