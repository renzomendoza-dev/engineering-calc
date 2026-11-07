package com.renzoproject.calc_api.mechanical.standpipe;

import java.util.List;

/**
 * Result of a standpipe hydraulic calculation, mirroring calc-core StandpipeHydraulicResult.
 * <b>Units:</b> psi, US gpm, ft, in., US gallons, minutes.
 */
public record StandpipeHydraulicResponse(
		List<DemandLineDto> demandLines,
		double requiredFlowGpm,
		boolean requiredFlowCapped,
		double mostRemoteStandpipeFlowGpm,
		double flowAtSupplyGpm,
		double flowAddedAtSupplyGpm,
		boolean flowAddedAtSupplyAutomatic,
		double pressureAtSupplyPsi,
		double totalFrictionLossPsi,
		double totalElevationPsi,
		int requiredFdcInlets,
		String fdcInletSize,
		boolean manualSystem,
		double supplyDurationMinutes,
		double waterSupplyVolumeGal,
		List<StandpipeSegmentRowDto> segments,
		List<StandpipeCheckDto> checks) {

}
