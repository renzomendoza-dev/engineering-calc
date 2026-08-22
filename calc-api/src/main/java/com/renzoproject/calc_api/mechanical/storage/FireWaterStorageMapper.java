package com.renzoproject.calc_api.mechanical.storage;

import com.renzoproject.calc.core.mechanical.firepump.FirePumpUnits;
import com.renzoproject.calc.core.mechanical.storage.FireWaterStorageInput;
import com.renzoproject.calc.core.mechanical.storage.FireWaterStorageResult;
import com.renzoproject.calc.core.mechanical.storage.HazardClassification;
import com.renzoproject.calc.core.mechanical.storage.StorageUnits;
import tech.units.indriya.quantity.Quantities;

/**
 * Pure mapping, no logic -- matches every other mapper in this codebase. Unit conversions
 * (GPM, gallons) happen here, not in the calculator, since calc-core already does its own
 * internal Indriya unit math and only needs raw doubles translated in/out.
 */
public final class FireWaterStorageMapper {

	private FireWaterStorageMapper() {
	}

	public static FireWaterStorageInput toCoreInput(FireWaterStorageRequest request) {
		double safetyMarginPercent = request.safetyMarginPercent() == null ? 0.0 : request.safetyMarginPercent();
		return new FireWaterStorageInput(
				Quantities.getQuantity(request.ratedPumpFlowGpm(), FirePumpUnits.GPM),
				toCoreHazardClassification(request.hazardClassification()),
				request.selectedDurationMinutes(),
				safetyMarginPercent);
	}

	public static FireWaterStorageResponse toResponse(FireWaterStorageResult result) {
		double volumeGallons = result.requiredStorageVolume().to(StorageUnits.GALLON_US).getValue().doubleValue();
		return new FireWaterStorageResponse(
				result.resolvedDurationMinutesMin(),
				result.resolvedDurationMinutesMax(),
				result.durationMinutesUsed(),
				result.usedConservativeDefault(),
				volumeGallons);
	}

	private static HazardClassification toCoreHazardClassification(HazardClassificationDto dto) {
		return switch (dto) {
			case LIGHT_HAZARD -> HazardClassification.LIGHT_HAZARD;
			case ORDINARY_HAZARD -> HazardClassification.ORDINARY_HAZARD;
			case EXTRA_HAZARD -> HazardClassification.EXTRA_HAZARD;
		};
	}

}
