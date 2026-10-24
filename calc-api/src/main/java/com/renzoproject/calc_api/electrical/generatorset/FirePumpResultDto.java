package com.renzoproject.calc_api.electrical.generatorset;

import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingResult.FirePumpResult;

import java.util.List;

/**
 * What Article 6.95 asks of the fire pump on this generator, mirroring calc-core's
 * {@code GeneratorSetSizingResult.FirePumpResult}. <b>Units:</b> amperes, percent.
 *
 * @param pumpFullLoadAmps                 the fire pump motor's full-load current at the system voltage
 * @param jockeyFullLoadAmps               the pressure maintenance pump's; blank when none
 * @param accessoryAmps                    the accessory equipment's; blank when none
 * @param pumpLockedRotorAmps              the fire pump motor's locked-rotor current at the system voltage; blank
 *                                         when none was entered or published
 * @param minimumConductorAmpacityAmps     125% of the pump and jockey full-load currents plus 100% of the accessories
 * @param conductorBasis                   how that was built, with the section it comes from
 * @param conductor                        the conductors sized for it; blank when no run was given or none fits
 * @param cableStartingDropPercent         the drop in those conductors at starting; blank without a run length
 * @param cableRunningDropPercent          the drop in them with the pump at 115% of full load
 * @param runningDropWithinLimit           whether that is within 5%
 * @param generatorStartingDropPercent     the dip the pump's step causes on the selected set
 * @param controllerStartingDropPercent    generator dip plus cable drop, as seen at the controller line terminals
 * @param startingDropWithinLimit          whether that is within 15%
 * @param generatorCurrentPercentOfPumpFla the selected set's nameplate current as a percent of the pump's
 *                                         full-load current
 * @param generatorExceedsExceptionLevel   whether that is above 225%, which brings 6.95.1.6(C) Exception No. 2 into play
 * @param voltageDropBasis                 where the 15% and 5% come from and how the drops were worked out
 * @param requirements                     Article 6.95 requirements to check off
 */
public record FirePumpResultDto(
		double pumpFullLoadAmps,
		Double jockeyFullLoadAmps,
		Double accessoryAmps,
		Double pumpLockedRotorAmps,
		double minimumConductorAmpacityAmps,
		String conductorBasis,
		FirePumpConductorDto conductor,
		Double cableStartingDropPercent,
		Double cableRunningDropPercent,
		Boolean runningDropWithinLimit,
		Double generatorStartingDropPercent,
		Double controllerStartingDropPercent,
		Boolean startingDropWithinLimit,
		Double generatorCurrentPercentOfPumpFla,
		Boolean generatorExceedsExceptionLevel,
		String voltageDropBasis,
		List<RequirementDto> requirements) {

	public static FirePumpResultDto from(FirePumpResult result) {
		return new FirePumpResultDto(
				result.pumpFullLoadAmps(),
				result.jockeyFullLoadAmps(),
				result.accessoryAmps(),
				result.pumpLockedRotorAmps(),
				result.minimumConductorAmpacityAmps(),
				result.conductorBasis(),
				result.conductor() == null ? null : FirePumpConductorDto.from(result.conductor()),
				result.cableStartingDropPercent(),
				result.cableRunningDropPercent(),
				result.runningDropWithinLimit(),
				result.generatorStartingDropPercent(),
				result.controllerStartingDropPercent(),
				result.startingDropWithinLimit(),
				result.generatorCurrentPercentOfPumpFla(),
				result.generatorExceedsExceptionLevel(),
				result.voltageDropBasis(),
				result.requirements().stream()
						.map(requirement -> new RequirementDto(requirement.citation(), requirement.text()))
						.toList());
	}

}
