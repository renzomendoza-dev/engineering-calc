package com.renzoproject.calc_api.electrical.generatorset;

import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingResult.LoadRow;

/**
 * One line of the load schedule, mirroring calc-core's {@code GeneratorSetSizingResult.LoadRow}.
 * <b>Units:</b> volt-amperes, percent.
 *
 * @param kind                 LIGHTING, RECEPTACLE, OTHER or MOTOR
 * @param label                name from the request, with the quantity for motors
 * @param connectedVoltAmperes load before any demand factor
 * @param demandFactorPercent  the factor actually applied (the effective one where a stepped table was used)
 * @param demandVoltAmperes    load after the demand factor
 * @param continuous           whether the load runs three hours or more
 * @param basis                the PEC sections and figures this line came from
 */
public record LoadRowDto(
		String kind,
		String label,
		double connectedVoltAmperes,
		double demandFactorPercent,
		double demandVoltAmperes,
		boolean continuous,
		String basis) {

	public static LoadRowDto from(LoadRow row) {
		return new LoadRowDto(
				row.kind().name(), row.label(), row.connectedVoltAmperes(), row.demandFactorPercent(),
				row.demandVoltAmperes(), row.continuous(), row.basis());
	}

}
