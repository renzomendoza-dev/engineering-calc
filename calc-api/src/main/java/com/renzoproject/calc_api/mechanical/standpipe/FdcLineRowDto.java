package com.renzoproject.calc_api.mechanical.standpipe;

import java.util.List;

/**
 * Result for one fire department connection, mirroring calc-core
 * {@code StandpipeHydraulicResult.FdcLineRow}. <b>Units:</b> psi, gpm. In each row of
 * {@code segments} the "remote end" is the system side and the "supply end" the connection side.
 */
public record FdcLineRowDto(
		String label,
		double flowGpm,
		Integer inlets,
		List<StandpipeSegmentRowDto> segments,
		double totalFrictionLossPsi,
		double totalElevationPsi,
		double pressureAtSystemPsi,
		double pressureAtFdcPsi) {

}
