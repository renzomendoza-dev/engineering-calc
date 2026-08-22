package com.renzoproject.calc_api.mechanical.storage;

import com.renzoproject.calc.core.mechanical.pipe.PipeUnits;
import com.renzoproject.calc.core.mechanical.storage.DemandBasis;
import com.renzoproject.calc.core.mechanical.storage.DomesticWaterStorageInput;
import com.renzoproject.calc.core.mechanical.storage.DomesticWaterStorageResult;
import com.renzoproject.calc.core.mechanical.storage.SystemType;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DomesticWaterStorageMapperTest {

	private static final double DELTA = 1e-9;

	@Test
	void toCoreInput_occupantLoad_mapsAllFieldsDirectly() {
		DomesticWaterStorageRequest request = new DomesticWaterStorageRequest(
				DemandBasisDto.OCCUPANT_LOAD, 100, "RESIDENTIAL_DWELLING", null, null, 24.0, 10.0);

		DomesticWaterStorageInput input = DomesticWaterStorageMapper.toCoreInput(request);

		assertEquals(DemandBasis.OCCUPANT_LOAD, input.demandBasis());
		assertEquals(100, input.occupantCount());
		assertEquals("RESIDENTIAL_DWELLING", input.occupancyType());
		assertNull(input.totalFixtureUnits());
		assertNull(input.systemType());
		assertEquals(24.0, input.storageDurationHours(), DELTA);
		assertEquals(10.0, input.safetyMarginPercent(), DELTA);
	}

	@Test
	void toCoreInput_fixtureUnit_flushTank_mapsSystemTypeCorrectly() {
		DomesticWaterStorageRequest request = new DomesticWaterStorageRequest(
				DemandBasisDto.FIXTURE_UNIT, null, null, 100.0, SystemTypeDto.FLUSH_TANK, 1.0, 0.0);

		DomesticWaterStorageInput input = DomesticWaterStorageMapper.toCoreInput(request);

		assertEquals(DemandBasis.FIXTURE_UNIT, input.demandBasis());
		assertEquals(100.0, input.totalFixtureUnits(), DELTA);
		assertEquals(SystemType.FLUSH_TANK, input.systemType());
	}

	@Test
	void toCoreInput_fixtureUnit_flushValve_mapsSystemTypeCorrectly() {
		DomesticWaterStorageRequest request = new DomesticWaterStorageRequest(
				DemandBasisDto.FIXTURE_UNIT, null, null, 100.0, SystemTypeDto.FLUSH_VALVE, 1.0, 0.0);

		DomesticWaterStorageInput input = DomesticWaterStorageMapper.toCoreInput(request);

		assertEquals(SystemType.FLUSH_VALVE, input.systemType());
	}

	@Test
	void toCoreInput_omittedSafetyMargin_defaultsToZero() {
		DomesticWaterStorageRequest request = new DomesticWaterStorageRequest(
				DemandBasisDto.OCCUPANT_LOAD, 100, "RESIDENTIAL_DWELLING", null, null, 24.0, null);

		DomesticWaterStorageInput input = DomesticWaterStorageMapper.toCoreInput(request);

		assertEquals(0.0, input.safetyMarginPercent(), DELTA);
	}

	@Test
	void toResponse_convertsToLitresPerSecondAndCubicMetres() {
		DomesticWaterStorageResult result = new DomesticWaterStorageResult(
				Quantities.getQuantity(1.0, PipeUnits.LITRE_PER_SECOND),
				Quantities.getQuantity(1000.0, Units.LITRE));

		DomesticWaterStorageResponse response = DomesticWaterStorageMapper.toResponse(result);

		assertEquals(1.0, response.resolvedDemandFlowRateLps(), DELTA);
		assertEquals(1.0, response.requiredStorageVolumeM3(), DELTA);
	}

}
