package com.renzoproject.calc_api.mechanical.storage;

import com.renzoproject.calc.core.mechanical.pipe.PipeUnits;
import com.renzoproject.calc.core.mechanical.storage.DemandBasis;
import com.renzoproject.calc.core.mechanical.storage.DomesticWaterStorageInput;
import com.renzoproject.calc.core.mechanical.storage.DomesticWaterStorageResult;
import com.renzoproject.calc.core.mechanical.storage.SystemType;
import tech.units.indriya.unit.Units;

/**
 * Pure mapping, no logic -- matches every other mapper in this codebase. Unit conversions
 * (L/s, m3) happen here, not in the calculator, since calc-core already does its own internal
 * Indriya unit math and only needs raw doubles translated in/out.
 */
public final class DomesticWaterStorageMapper {

	private DomesticWaterStorageMapper() {
	}

	public static DomesticWaterStorageInput toCoreInput(DomesticWaterStorageRequest request) {
		double safetyMarginPercent = request.safetyMarginPercent() == null ? 0.0 : request.safetyMarginPercent();
		return new DomesticWaterStorageInput(
				toCoreDemandBasis(request.demandBasis()),
				request.occupantCount(),
				request.occupancyType(),
				request.totalFixtureUnits(),
				toCoreSystemType(request.systemType()),
				request.storageDurationHours(),
				safetyMarginPercent);
	}

	public static DomesticWaterStorageResponse toResponse(DomesticWaterStorageResult result) {
		double flowRateLps = result.resolvedDemandFlowRate().to(PipeUnits.LITRE_PER_SECOND).getValue().doubleValue();
		double volumeM3 = result.requiredStorageVolume().to(Units.CUBIC_METRE).getValue().doubleValue();
		return new DomesticWaterStorageResponse(flowRateLps, volumeM3);
	}

	private static DemandBasis toCoreDemandBasis(DemandBasisDto dto) {
		return switch (dto) {
			case OCCUPANT_LOAD -> DemandBasis.OCCUPANT_LOAD;
			case FIXTURE_UNIT -> DemandBasis.FIXTURE_UNIT;
		};
	}

	private static SystemType toCoreSystemType(SystemTypeDto dto) {
		if (dto == null) {
			return null;
		}
		return switch (dto) {
			case FLUSH_TANK -> SystemType.FLUSH_TANK;
			case FLUSH_VALVE -> SystemType.FLUSH_VALVE;
		};
	}

}
