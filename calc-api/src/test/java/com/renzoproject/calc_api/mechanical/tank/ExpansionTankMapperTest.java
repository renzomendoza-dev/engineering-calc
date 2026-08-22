package com.renzoproject.calc_api.mechanical.tank;

import com.renzoproject.calc.core.mechanical.tank.ExpansionTankInput;
import com.renzoproject.calc.core.mechanical.tank.ExpansionTankResult;
import com.renzoproject.calc.core.mechanical.tank.SystemType;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpansionTankMapperTest {

	private static final double DELTA = 1e-6;

	private static ExpansionTankRequest baseRequest(Double additionalVolumeFactor) {
		return new ExpansionTankRequest(
				SystemTypeDto.DOMESTIC_WATER_HEATER,
				190.0,
				10.0,
				20.0,
				additionalVolumeFactor,
				10.0,
				60.0,
				300.0,
				550.0,
				0.0);
	}

	@Test
	void toCoreInput_mapsUnitConversionsCorrectly() {
		ExpansionTankInput input = ExpansionTankMapper.toCoreInput(baseRequest(1.1));

		assertEquals(SystemType.DOMESTIC_WATER_HEATER, input.systemType());
		// 190 L -> 0.19 m3 round trip.
		assertEquals(0.19, input.primaryVesselVolume().to(Units.CUBIC_METRE).getValue().doubleValue(), DELTA);
		assertEquals(10.0, input.estimatedPipingLength().to(Units.METRE).getValue().doubleValue(), DELTA);
		assertEquals(20.0, input.averagePipeDiameterMm(), DELTA);
		assertEquals(1.1, input.additionalVolumeFactor(), DELTA);
		assertEquals(10.0, input.coldWaterTemperature().to(Units.CELSIUS).getValue().doubleValue(), DELTA);
		assertEquals(60.0, input.hotWaterTemperature().to(Units.CELSIUS).getValue().doubleValue(), DELTA);
		// 300 kPa -> 300000 Pa round trip.
		assertEquals(300000.0, input.fillPressureGauge().to(Units.PASCAL).getValue().doubleValue(), DELTA);
		assertEquals(550000.0, input.maxOperatingPressureGauge().to(Units.PASCAL).getValue().doubleValue(), DELTA);
		assertEquals(0.0, input.altitude().to(Units.METRE).getValue().doubleValue(), DELTA);
	}

	@Test
	void toCoreInput_additionalVolumeFactorOmitted_defaultsToOne() {
		ExpansionTankInput input = ExpansionTankMapper.toCoreInput(baseRequest(null));

		assertEquals(1.0, input.additionalVolumeFactor(), DELTA);
	}

	@Test
	void toResponse_mapsUnitConversionsCorrectly() {
		ExpansionTankResult result = new ExpansionTankResult(
				Quantities.getQuantity(0.193, Units.CUBIC_METRE),
				Quantities.getQuantity(0.0032, Units.CUBIC_METRE),
				Quantities.getQuantity(0.0084, Units.CUBIC_METRE),
				999.7,
				983.2,
				Quantities.getQuantity(401325.0, Units.PASCAL),
				Quantities.getQuantity(651325.0, Units.PASCAL));

		ExpansionTankResponse response = ExpansionTankMapper.toResponse(result);

		// 0.193 m3 -> 193 L round trip.
		assertEquals(193.0, response.derivedSystemVolumeLiters(), DELTA);
		assertEquals(3.2, response.expansionVolumeLiters(), DELTA);
		assertEquals(8.4, response.requiredTankVolumeLiters(), DELTA);
		assertEquals(999.7, response.coldWaterDensityKgM3(), DELTA);
		assertEquals(983.2, response.hotWaterDensityKgM3(), DELTA);
		// 401325 Pa -> 401.325 kPa round trip.
		assertEquals(401.325, response.fillPressureAbsoluteKpa(), DELTA);
		assertEquals(651.325, response.maxOperatingPressureAbsoluteKpa(), DELTA);
	}

}
