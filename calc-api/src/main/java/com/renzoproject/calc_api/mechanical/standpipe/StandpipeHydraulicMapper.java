package com.renzoproject.calc_api.mechanical.standpipe;

import com.renzoproject.calc.core.mechanical.standpipe.CombinedHazard;
import com.renzoproject.calc.core.mechanical.standpipe.HoseConnectionSize;
import com.renzoproject.calc.core.mechanical.standpipe.PipeRole;
import com.renzoproject.calc.core.mechanical.standpipe.SprinklerProtection;
import com.renzoproject.calc.core.mechanical.standpipe.StandpipeClass;
import com.renzoproject.calc.core.mechanical.standpipe.StandpipeFitting;
import com.renzoproject.calc.core.mechanical.standpipe.StandpipeHydraulicInput;
import com.renzoproject.calc.core.mechanical.standpipe.StandpipeHydraulicResult;
import com.renzoproject.calc.core.mechanical.standpipe.StandpipeHydraulicResult.DemandLine;
import com.renzoproject.calc.core.mechanical.standpipe.StandpipeHydraulicResult.SegmentRow;
import com.renzoproject.calc.core.mechanical.standpipe.StandpipeSegment;
import com.renzoproject.calc_api.common.EnumParsing;

/**
 * Pure mapping between the request/response DTOs and calc-core, no logic.
 */
public final class StandpipeHydraulicMapper {

	private StandpipeHydraulicMapper() {
	}

	public static StandpipeHydraulicInput toCoreInput(StandpipeHydraulicRequest request) {
		return new StandpipeHydraulicInput(
				EnumParsing.parse(StandpipeClass.class, request.standpipeClass(), "standpipe class"),
				blank(request.sprinklerProtection()) ? null
						: EnumParsing.parse(SprinklerProtection.class, request.sprinklerProtection(), "sprinkler protection"),
				blank(request.sprinklerHazard()) ? null
						: EnumParsing.parse(CombinedHazard.class, request.sprinklerHazard(), "sprinkler hazard"),
				request.sprinklerDemandGpm(),
				request.additionalStandpipes(),
				request.floorAreaOver80000SqFt(),
				request.horizontalStandpipeOnThreeConnections(),
				request.fireDepartmentConnectionInlets(),
				request.supplyDurationMinutes(),
				request.flowAddedAtSupplyGpm(),
				request.segments().stream().map(StandpipeHydraulicMapper::toCoreSegment).toList());
	}

	private static boolean blank(String value) {
		return value == null || value.isBlank();
	}

	private static StandpipeSegment toCoreSegment(StandpipeSegmentDto segment) {
		return new StandpipeSegment(
				segment.label(),
				EnumParsing.parse(PipeRole.class, segment.role(), "pipe role"),
				segment.flowAddedGpm(),
				blank(segment.hoseConnectionSize()) ? null
						: EnumParsing.parse(HoseConnectionSize.class, segment.hoseConnectionSize(), "hose connection size"),
				segment.pipeMaterial(),
				segment.schedule(),
				segment.nominalSize(),
				segment.cFactor(),
				segment.pipeLengthFt(),
				segment.elevationRiseFt(),
				segment.fittings() == null ? null : segment.fittings().stream()
						.map(fitting -> new StandpipeFitting(fitting.fittingType(), fitting.quantity(), fitting.explicitEquivalentLengthFt()))
						.toList());
	}

	public static StandpipeHydraulicResponse toResponse(StandpipeHydraulicResult result) {
		return new StandpipeHydraulicResponse(
				result.demandLines().stream().map(StandpipeHydraulicMapper::toDto).toList(),
				result.requiredFlowGpm(),
				result.requiredFlowCapped(),
				result.mostRemoteStandpipeFlowGpm(),
				result.flowAtSupplyGpm(),
				result.flowAddedAtSupplyGpm(),
				result.flowAddedAtSupplyAutomatic(),
				result.pressureAtSupplyPsi(),
				result.totalFrictionLossPsi(),
				result.totalElevationPsi(),
				result.requiredFdcInlets(),
				result.supplyDurationMinutes(),
				result.waterSupplyVolumeGal(),
				result.segments().stream().map(StandpipeHydraulicMapper::toDto).toList(),
				result.checks().stream().map(StandpipeCheckDto::from).toList());
	}

	private static DemandLineDto toDto(DemandLine line) {
		return new DemandLineDto(line.clause(), line.description(), line.flowGpm());
	}

	private static StandpipeSegmentRowDto toDto(SegmentRow row) {
		return new StandpipeSegmentRowDto(
				row.label(), row.role().name(), row.hoseConnectionSize() == null ? null : row.hoseConnectionSize().name(), row.nominalSize(),
				row.insideDiameterInch(), row.cFactor(), row.flowAddedGpm(), row.flowGpm(), row.velocityFtPerSec(), row.pipeLengthFt(),
				row.fittingsLengthFt(), row.frictionPsiPerFt(), row.frictionLossPsi(), row.elevationRiseFt(), row.elevationPsi(),
				row.pressureAtRemoteEndPsi(), row.pressureAtSupplyEndPsi(), row.residualRaised());
	}

}
