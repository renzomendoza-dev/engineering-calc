package com.renzoproject.calc_api.mechanical.standpipe;

import java.util.List;

/**
 * The standpipe reference tables with the edition they were read from, so a page can say so.
 *
 * @param edition  edition the tables were transcribed from
 * @param note     the file's own caution about its confidence
 * @param fittings equivalent length chart
 * @param cFactors Hazen-Williams C values
 */
public record StandpipeReferenceResponse(
		String edition,
		String note,
		List<StandpipeFittingEntryDto> fittings,
		List<StandpipeCFactorEntryDto> cFactors) {

}
