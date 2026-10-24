package com.renzoproject.calc_api.electrical.generatorset;

import java.util.List;

/**
 * The choices generator set sizing offers, read from calc-core's reference data so the form never
 * keeps its own copy of a table. {@code standardSizesNote} says the sizes are not PEC data.
 *
 * @param lightingOccupancies          Table 2.20.2.3 occupancies
 * @param lightingAreaTypes            area types that take their own unit load
 * @param standbyClasses               Chapter 7 articles the source can fall under
 * @param optionalTransfers            transfer modes for optional standby
 * @param standardSizesKva             standard generator set sizes in kVA, ascending
 * @param standardSizesNote            where those sizes come from
 * @param defaultGeneratorPowerFactor  rating power factor to start the form with
 * @param defaultGeneratorPowerFactorNote why that default, and that it is not a PEC value
 * @param defaultSubtransientReactancePercent the generator reactance X"d to start the form with, in percent
 * @param defaultMaximumVoltageDipPercent the voltage dip limit to start the form with, in percent
 * @param motorStartingDefaultsNote       why those two defaults, and that neither is a PEC value for this use
 */
public record GeneratorSetReferenceResponse(
		List<LightingOccupancyDto> lightingOccupancies,
		List<LightingAreaTypeDto> lightingAreaTypes,
		List<StandbyClassOptionDto> standbyClasses,
		List<OptionalTransferOptionDto> optionalTransfers,
		List<Double> standardSizesKva,
		String standardSizesNote,
		double defaultGeneratorPowerFactor,
		String defaultGeneratorPowerFactorNote,
		double defaultSubtransientReactancePercent,
		double defaultMaximumVoltageDipPercent,
		String motorStartingDefaultsNote) {

}
