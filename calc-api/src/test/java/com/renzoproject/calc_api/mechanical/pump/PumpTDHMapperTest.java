package com.renzoproject.calc_api.mechanical.pump;

import com.renzoproject.calc.core.mechanical.pipe.FlowRegime;
import com.renzoproject.calc.core.mechanical.pipe.FrictionFactorMethod;
import com.renzoproject.calc.core.mechanical.pipe.NominalSize;
import com.renzoproject.calc.core.mechanical.pipe.PipePressureLossResult;
import com.renzoproject.calc.core.mechanical.pipe.RawDiameter;
import com.renzoproject.calc.core.mechanical.pump.PipeSegmentSpec;
import com.renzoproject.calc.core.mechanical.pump.PumpTDHInput;
import com.renzoproject.calc.core.mechanical.pump.PumpTDHResult;
import com.renzoproject.calc.core.mechanical.pump.SegmentLossDetail;
import com.renzoproject.calc.core.mechanical.pump.SuctionCondition;
import com.renzoproject.calc_api.mechanical.pipe.DiameterSpecTypeDto;
import com.renzoproject.calc_api.mechanical.pipe.FrictionFactorMethodDto;
import com.renzoproject.calc_api.mechanical.pipe.PipePressureLossMapper;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.MetricPrefix;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PumpTDHMapperTest {

	private static final double DELTA = 1e-9;

	private static PipeSegmentSpecDto nominalSegmentDto(String material, String label, double lengthM) {
		return new PipeSegmentSpecDto(DiameterSpecTypeDto.NOMINAL, material, "SCH40", label, null, null, lengthM, FrictionFactorMethodDto.SWAMEE_JAIN);
	}

	@Test
	void toCoreInput_flooded_mapsSegmentsAndKpaToPaCorrectly() {
		PumpTDHRequest request = new PumpTDHRequest(
				"WATER", 20.0, 10.0, "L/s",
				SuctionConditionDto.FLOODED, null, 2.0, List.of(nominalSegmentDto("GI", "2", 10.0)),
				15.0, 200.0, List.of(nominalSegmentDto("GI", "3", 20.0)),
				false);

		PumpTDHInput input = PumpTDHMapper.toCoreInput(request);

		assertEquals(SuctionCondition.FLOODED, input.suctionCondition());
		assertEquals(2.0, input.staticSuctionHeadFlooded().to(Units.METRE).getValue().doubleValue(), DELTA);
		assertNull(input.staticSuctionLift());
		assertEquals(1, input.suctionSegments().size());
		assertEquals(10.0, input.suctionSegments().get(0).length().to(Units.METRE).getValue().doubleValue(), DELTA);
		assertEquals(1, input.dischargeSegments().size());
		// 200 kPa = 200000 Pa
		assertEquals(200000.0, input.requiredResidualPressure().to(Units.PASCAL).getValue().doubleValue(), DELTA);
	}

	@Test
	void toCoreInput_lift_withEmptySegmentLists_mapsFieldsCorrectly() {
		PumpTDHRequest request = new PumpTDHRequest(
				"WATER", 20.0, 10.0, "L/s",
				SuctionConditionDto.LIFT, 3.0, null, List.of(),
				15.0, 0.0, List.of(),
				false);

		PumpTDHInput input = PumpTDHMapper.toCoreInput(request);

		assertEquals(SuctionCondition.LIFT, input.suctionCondition());
		assertEquals(3.0, input.staticSuctionLift().to(Units.METRE).getValue().doubleValue(), DELTA);
		assertNull(input.staticSuctionHeadFlooded());
		assertTrue(input.suctionSegments().isEmpty());
		assertTrue(input.dischargeSegments().isEmpty());
	}

	@Test
	void toResponse_embedsPipePressureLossResponseViaSharedMapper() {
		PipeSegmentSpec segment = new PipeSegmentSpec(
				new NominalSize("GI", "SCH40", "2"), Quantities.getQuantity(10.0, Units.METRE), FrictionFactorMethod.SWAMEE_JAIN);
		PipePressureLossResult pressureLossResult = new PipePressureLossResult(
				Quantities.getQuantity(2.0, Units.METRE_PER_SECOND), 100000.0, FlowRegime.TURBULENT, false, 0.02,
				Quantities.getQuantity(1.5, Units.METRE), Quantities.getQuantity(15000.0, Units.PASCAL));
		SegmentLossDetail detail = new SegmentLossDetail(segment, pressureLossResult);

		PumpTDHResult result = new PumpTDHResult(
				Quantities.getQuantity(13.0, Units.METRE),
				Quantities.getQuantity(1.5, Units.METRE),
				Quantities.getQuantity(0.0, Units.METRE),
				Quantities.getQuantity(0.0, Units.METRE),
				Quantities.getQuantity(0.0, Units.METRE),
				Quantities.getQuantity(14.5, Units.METRE),
				false,
				List.of(detail),
				List.of());

		PumpTDHResponse response = PumpTDHMapper.toResponse(result);

		assertEquals(1, response.suctionSegmentDetails().size());
		var detailDto = response.suctionSegmentDetails().get(0);
		assertEquals(DiameterSpecTypeDto.NOMINAL, detailDto.segment().diameterSpecType());
		assertEquals("GI", detailDto.segment().nominalMaterial());
		assertEquals("2", detailDto.segment().nominalLabel());
		assertEquals(10.0, detailDto.segment().lengthMeters(), DELTA);
		// Confirm the embedded result is exactly what PipePressureLossMapper.toResponse would
		// produce for this same core result -- proving reuse, not a re-implementation.
		var expectedEmbedded = PipePressureLossMapper.toResponse(pressureLossResult);
		assertEquals(expectedEmbedded, detailDto.result());
		assertTrue(response.dischargeSegmentDetails().isEmpty());
	}

	@Test
	void toResponse_rawDiameterSegment_echoesBackAsRawInMetres() {
		PipeSegmentSpec segment = new PipeSegmentSpec(
				new RawDiameter(Quantities.getQuantity(50.0, MetricPrefix.MILLI(Units.METRE))),
				Quantities.getQuantity(5.0, Units.METRE), FrictionFactorMethod.SWAMEE_JAIN);
		PipePressureLossResult pressureLossResult = new PipePressureLossResult(
				Quantities.getQuantity(1.0, Units.METRE_PER_SECOND), 1000.0, FlowRegime.LAMINAR, false, 0.064,
				Quantities.getQuantity(0.1, Units.METRE), Quantities.getQuantity(1000.0, Units.PASCAL));
		SegmentLossDetail detail = new SegmentLossDetail(segment, pressureLossResult);

		PumpTDHResult result = new PumpTDHResult(
				Quantities.getQuantity(1.0, Units.METRE), Quantities.getQuantity(0.0, Units.METRE),
				Quantities.getQuantity(0.1, Units.METRE), Quantities.getQuantity(0.0, Units.METRE),
				Quantities.getQuantity(0.0, Units.METRE), Quantities.getQuantity(1.1, Units.METRE),
				false, List.of(), List.of(detail));

		PumpTDHResponse response = PumpTDHMapper.toResponse(result);

		var detailDto = response.dischargeSegmentDetails().get(0);
		assertEquals(DiameterSpecTypeDto.RAW, detailDto.segment().diameterSpecType());
		assertEquals(0.05, detailDto.segment().rawDiameterValue(), DELTA);
		assertEquals("m", detailDto.segment().rawDiameterUnit());
	}

}
