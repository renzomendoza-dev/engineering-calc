package com.renzoproject.calc_api.mechanical.standpipe;

import java.util.List;

/**
 * The standpipe reference tables with the edition they were read from, so a page can say so.
 *
 * @param edition  edition the tables were transcribed from
 * @param note     the file's own caution about its confidence
 * @param fittings equivalent length chart
 * @param cFactors Hazen-Williams C values
 * @param hoseNozzleNote the Annex's caution about using Table A.10.2.6
 * @param hoseNozzleCombinations Table A.10.2.6, hose and nozzle combinations with the outlet pressure each needs
 */
public record StandpipeReferenceResponse(
		String edition,
		String note,
		List<StandpipeFittingEntryDto> fittings,
		List<StandpipeCFactorEntryDto> cFactors,
		String hoseNozzleNote,
		List<StandpipeHoseNozzleEntryDto> hoseNozzleCombinations) {

}
