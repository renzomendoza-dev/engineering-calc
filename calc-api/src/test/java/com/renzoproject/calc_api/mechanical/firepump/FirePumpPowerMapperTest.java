package com.renzoproject.calc_api.mechanical.firepump;

import com.renzoproject.calc.core.mechanical.firepump.CandidatePumpCurve;
import com.renzoproject.calc.core.mechanical.firepump.DriverType;
import com.renzoproject.calc.core.mechanical.firepump.FirePumpPowerInput;
import com.renzoproject.calc.core.mechanical.firepump.FirePumpPowerResult;
import com.renzoproject.calc.core.mechanical.firepump.FirePumpUnits;
import com.renzoproject.calc.core.mechanical.firepump.FullCurve;
import com.renzoproject.calc.core.mechanical.firepump.PowerAtPoint;
import com.renzoproject.calc.core.mechanical.firepump.PumpCurvePoint;
import com.renzoproject.calc.core.mechanical.firepump.RatedPointOnly;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class FirePumpPowerMapperTest {

	private static final double DELTA = 1e-9;

	@Test
	void toCoreInput_ratedPointOnly_buildsRatedPointOnlyVariantAsElectric() {
		FirePumpPowerRequest request = new FirePumpPowerRequest(
				FirePumpPowerInputTypeDto.RATED_POINT_ONLY,
				500.0, 100.0,
				null, null, null,
				0.75, 1.0);

		FirePumpPowerInput input = FirePumpPowerMapper.toCoreInput(request);

		assertInstanceOf(RatedPointOnly.class, input);
		RatedPointOnly ratedOnly = (RatedPointOnly) input;
		assertEquals(500.0, ratedOnly.flow().to(FirePumpUnits.GPM).getValue().doubleValue(), DELTA);
		assertEquals(100.0, ratedOnly.pressure().to(FirePumpUnits.PSI).getValue().doubleValue(), DELTA);
		assertEquals(0.75, ratedOnly.pumpEfficiency(), DELTA);
		assertEquals(1.0, ratedOnly.specificGravity(), DELTA);
		assertEquals(DriverType.ELECTRIC, ratedOnly.driverType());
	}

	@Test
	void toCoreInput_fullCurve_buildsFullCurveVariantAsElectric() {
		FirePumpPowerRequest request = new FirePumpPowerRequest(
				FirePumpPowerInputTypeDto.FULL_CURVE,
				null, null,
				500.0, 100.0,
				List.of(new PumpCurvePointDto(0.0, 135.0), new PumpCurvePointDto(750.0, 75.0)),
				0.75, null);

		FirePumpPowerInput input = FirePumpPowerMapper.toCoreInput(request);

		assertInstanceOf(FullCurve.class, input);
		FullCurve fullCurve = (FullCurve) input;
		assertEquals(500.0, fullCurve.curve().ratedFlow().to(FirePumpUnits.GPM).getValue().doubleValue(), DELTA);
		assertEquals(100.0, fullCurve.curve().ratedPressure().to(FirePumpUnits.PSI).getValue().doubleValue(), DELTA);
		assertEquals(2, fullCurve.curve().curvePoints().size());
		assertEquals(0.75, fullCurve.pumpEfficiency(), DELTA);
		// specificGravity omitted -> defaults to 1.0.
		assertEquals(1.0, fullCurve.specificGravity(), DELTA);
		assertEquals(DriverType.ELECTRIC, fullCurve.driverType());
	}

	@Test
	void toResponse_mapsEvaluatedPointsAndSummaryFields() {
		PowerAtPoint point = new PowerAtPoint(
				Quantities.getQuantity(500.0, FirePumpUnits.GPM),
				Quantities.getQuantity(100.0, FirePumpUnits.PSI),
				29.166666666666668, 38.88888888888889);
		FirePumpPowerResult result = new FirePumpPowerResult(List.of(point), 38.88888888888889, "40 HP", "some note");

		FirePumpPowerResponse response = FirePumpPowerMapper.toResponse(result);

		assertEquals(1, response.evaluatedPoints().size());
		assertEquals(500.0, response.evaluatedPoints().get(0).flowGpm(), DELTA);
		assertEquals(100.0, response.evaluatedPoints().get(0).pressurePsi(), DELTA);
		assertEquals(29.166666666666668, response.evaluatedPoints().get(0).waterHorsepower(), DELTA);
		assertEquals(38.88888888888889, response.evaluatedPoints().get(0).brakeHorsepower(), DELTA);
		assertEquals(38.88888888888889, response.maxBrakeHorsepower(), DELTA);
		assertEquals("40 HP", response.recommendedElectricMotorSize());
		assertEquals("some note", response.driverSizingNote());
	}

	@Test
	void toResponse_nullRecommendedMotorSizeAndNoteArePreserved() {
		PowerAtPoint point = new PowerAtPoint(
				Quantities.getQuantity(500.0, FirePumpUnits.GPM),
				Quantities.getQuantity(100.0, FirePumpUnits.PSI),
				29.166666666666668, 38.88888888888889);
		FirePumpPowerResult result = new FirePumpPowerResult(List.of(point), 38.88888888888889, null, null);

		FirePumpPowerResponse response = FirePumpPowerMapper.toResponse(result);

		assertNull(response.recommendedElectricMotorSize());
		assertNull(response.driverSizingNote());
	}

}
