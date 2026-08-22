package com.renzoproject.calc_api.mechanical.storage;

import com.renzoproject.calc.core.mechanical.firepump.FirePumpUnits;
import com.renzoproject.calc.core.mechanical.storage.FireWaterStorageInput;
import com.renzoproject.calc.core.mechanical.storage.FireWaterStorageResult;
import com.renzoproject.calc.core.mechanical.storage.HazardClassification;
import com.renzoproject.calc.core.mechanical.storage.StorageUnits;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FireWaterStorageMapperTest {

	private static final double DELTA = 1e-9;

	@Test
	void toCoreInput_mapsAllFieldsDirectly() {
		FireWaterStorageRequest request = new FireWaterStorageRequest(500.0, HazardClassificationDto.ORDINARY_HAZARD, 75.0, 20.0);

		FireWaterStorageInput input = FireWaterStorageMapper.toCoreInput(request);

		assertEquals(500.0, input.ratedPumpFlow().to(FirePumpUnits.GPM).getValue().doubleValue(), DELTA);
		assertEquals(HazardClassification.ORDINARY_HAZARD, input.hazardClassification());
		assertEquals(75.0, input.selectedDurationMinutes(), DELTA);
		assertEquals(20.0, input.safetyMarginPercent(), DELTA);
	}

	@Test
	void toCoreInput_omittedSelectedDuration_mapsToNull() {
		FireWaterStorageRequest request = new FireWaterStorageRequest(500.0, HazardClassificationDto.ORDINARY_HAZARD, null, 0.0);

		FireWaterStorageInput input = FireWaterStorageMapper.toCoreInput(request);

		assertNull(input.selectedDurationMinutes());
	}

	@Test
	void toCoreInput_omittedSafetyMargin_defaultsToZero() {
		FireWaterStorageRequest request = new FireWaterStorageRequest(500.0, HazardClassificationDto.LIGHT_HAZARD, null, null);

		FireWaterStorageInput input = FireWaterStorageMapper.toCoreInput(request);

		assertEquals(0.0, input.safetyMarginPercent(), DELTA);
	}

	@Test
	void toCoreInput_allThreeHazardClassifications_mapCorrectly() {
		assertEquals(HazardClassification.LIGHT_HAZARD,
				FireWaterStorageMapper.toCoreInput(new FireWaterStorageRequest(100.0, HazardClassificationDto.LIGHT_HAZARD, null, 0.0)).hazardClassification());
		assertEquals(HazardClassification.ORDINARY_HAZARD,
				FireWaterStorageMapper.toCoreInput(new FireWaterStorageRequest(100.0, HazardClassificationDto.ORDINARY_HAZARD, null, 0.0)).hazardClassification());
		assertEquals(HazardClassification.EXTRA_HAZARD,
				FireWaterStorageMapper.toCoreInput(new FireWaterStorageRequest(100.0, HazardClassificationDto.EXTRA_HAZARD, null, 0.0)).hazardClassification());
	}

	@Test
	void toResponse_mapsAllFieldsAndConvertsVolumeToGallons() {
		FireWaterStorageResult result = new FireWaterStorageResult(60.0, 90.0, 90.0, true,
				Quantities.getQuantity(45000.0, StorageUnits.GALLON_US));

		FireWaterStorageResponse response = FireWaterStorageMapper.toResponse(result);

		assertEquals(60.0, response.resolvedDurationMinutesMin(), DELTA);
		assertEquals(90.0, response.resolvedDurationMinutesMax(), DELTA);
		assertEquals(90.0, response.durationMinutesUsed(), DELTA);
		assertEquals(true, response.usedConservativeDefault());
		assertEquals(45000.0, response.requiredStorageVolumeGallons(), DELTA);
	}

}
