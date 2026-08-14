package com.renzoproject.calc_api.mechanical.firepump;

import com.renzoproject.calc.core.mechanical.firepump.FirePumpCurveValidationInput;
import com.renzoproject.calc.core.mechanical.firepump.FirePumpCurveValidationResult;
import com.renzoproject.calc.core.mechanical.firepump.FirePumpUnits;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FirePumpCurveValidationMapperTest {

	private static final double DELTA = 1e-9;

	@Test
	void toCoreInput_mapsCandidateCurveAndDemand() {
		FirePumpCurveValidationRequest request = new FirePumpCurveValidationRequest(
				1000.0, 100.0,
				List.of(new PumpCurvePointDto(0.0, 135.0), new PumpCurvePointDto(1500.0, 70.0)),
				1000.0, 100.0);

		FirePumpCurveValidationInput input = FirePumpCurveValidationMapper.toCoreInput(request);

		assertEquals(1000.0, input.candidate().ratedFlow().to(FirePumpUnits.GPM).getValue().doubleValue(), DELTA);
		assertEquals(100.0, input.candidate().ratedPressure().to(FirePumpUnits.PSI).getValue().doubleValue(), DELTA);
		assertEquals(2, input.candidate().curvePoints().size());
		assertEquals(0.0, input.candidate().curvePoints().get(0).flow().to(FirePumpUnits.GPM).getValue().doubleValue(), DELTA);
		assertEquals(135.0, input.candidate().curvePoints().get(0).pressure().to(FirePumpUnits.PSI).getValue().doubleValue(), DELTA);
		assertEquals(1000.0, input.demandFlow().to(FirePumpUnits.GPM).getValue().doubleValue(), DELTA);
		assertEquals(100.0, input.demandPressure().to(FirePumpUnits.PSI).getValue().doubleValue(), DELTA);
	}

	@Test
	void toResponse_mapsAllFields() {
		FirePumpCurveValidationResult result = new FirePumpCurveValidationResult(
				true,
				Quantities.getQuantity(135.0, FirePumpUnits.PSI), true,
				Quantities.getQuantity(70.0, FirePumpUnits.PSI), true,
				true);

		FirePumpCurveValidationResponse response = FirePumpCurveValidationMapper.toResponse(result);

		assertTrue(response.meetsCapacityDemand());
		assertEquals(135.0, response.churnPressurePsi(), DELTA);
		assertTrue(response.churnCompliant());
		assertEquals(70.0, response.overloadPressurePsi(), DELTA);
		assertTrue(response.overloadCompliant());
		assertTrue(response.overallCompliant());
	}

	@Test
	void toResponse_nonCompliantFlagsPreserved() {
		FirePumpCurveValidationResult result = new FirePumpCurveValidationResult(
				true,
				Quantities.getQuantity(145.0, FirePumpUnits.PSI), false,
				Quantities.getQuantity(70.0, FirePumpUnits.PSI), true,
				false);

		FirePumpCurveValidationResponse response = FirePumpCurveValidationMapper.toResponse(result);

		assertFalse(response.churnCompliant());
		assertFalse(response.overallCompliant());
	}

}
