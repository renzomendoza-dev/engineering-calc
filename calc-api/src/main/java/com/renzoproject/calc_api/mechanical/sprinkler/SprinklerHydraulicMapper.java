package com.renzoproject.calc_api.mechanical.sprinkler;

import com.renzoproject.calc.core.mechanical.sprinkler.DeviceLoss;
import com.renzoproject.calc.core.mechanical.sprinkler.GravityTankSupply;
import com.renzoproject.calc.core.mechanical.sprinkler.RemoteAreaSprinkler;
import com.renzoproject.calc.core.mechanical.sprinkler.SegmentFitting;
import com.renzoproject.calc.core.mechanical.sprinkler.SprinklerHazard;
import com.renzoproject.calc.core.mechanical.sprinkler.SprinklerHydraulicInput;
import com.renzoproject.calc.core.mechanical.sprinkler.SprinklerHydraulicResult;
import com.renzoproject.calc.core.mechanical.sprinkler.SprinklerPipeSegment;
import com.renzoproject.calc.core.mechanical.sprinkler.SprinklerSystemType;
import com.renzoproject.calc.core.mechanical.sprinkler.SupplyCurve;
import com.renzoproject.calc.core.mechanical.sprinkler.SupplyPipeRun;
import com.renzoproject.calc.core.mechanical.sprinkler.WaterSupply;

import java.util.List;

/**
 * Pure mapping, no logic -- matches every other mapper in this codebase. No unit conversion
 * happens here, unusually: calc-core's sprinkler package already works in psi/gpm/ft/in (NFPA 13's
 * units) and this endpoint's DTOs use the same, so values pass straight through. The enum
 * translations are explicit switches, same as {@code DuctSizingMapper}.
 */
public final class SprinklerHydraulicMapper {

	private SprinklerHydraulicMapper() {
	}

	public static SprinklerHydraulicInput toCoreInput(SprinklerHydraulicRequest request) {
		return new SprinklerHydraulicInput(
				toCoreSystemType(request.systemType()),
				toCoreHazard(request.hazard()),
				request.densityGpmPerSqFt(),
				request.designAreaSqFt(),
				request.hoseStreamGpm(),
				request.hoseStreamNodeId(),
				request.assumedStartPressurePsi(),
				toCoreSprinklers(request.sprinklers()),
				toCoreSegments(request.segments()),
				toCoreWaterSupply(request.waterSupply()),
				toCoreDeviceLosses(request.deviceLosses()));
	}

	public static SprinklerHydraulicResponse toResponse(SprinklerHydraulicResult result) {
		return SprinklerHydraulicResponse.from(result);
	}

	private static List<RemoteAreaSprinkler> toCoreSprinklers(List<RemoteAreaSprinklerDto> sprinklers) {
		return sprinklers.stream()
				.map(dto -> new RemoteAreaSprinkler(dto.nodeId(), dto.kFactor(), dto.coverageAreaSqFt()))
				.toList();
	}

	private static List<SprinklerPipeSegment> toCoreSegments(List<SprinklerPipeSegmentDto> segments) {
		return segments.stream().map(SprinklerHydraulicMapper::toCoreSegment).toList();
	}

	private static List<SegmentFitting> toCoreFittings(List<SegmentFittingDto> fittings) {
		if (fittings == null) {
			return List.of();
		}
		return fittings.stream()
				.map(fitting -> new SegmentFitting(
						fitting.fittingType(),
						fitting.quantity(),
						fitting.explicitEquivalentLengthFt()))
				.toList();
	}

	private static SprinklerPipeSegment toCoreSegment(SprinklerPipeSegmentDto dto) {
		List<SegmentFitting> fittings = toCoreFittings(dto.fittings());

		return new SprinklerPipeSegment(
				dto.fromNodeId(),
				dto.toNodeId(),
				dto.pipeMaterial(),
				dto.schedule(),
				dto.nominalSize(),
				dto.cFactor(),
				dto.pipeLengthFt(),
				dto.elevationChangeFt(),
				fittings);
	}

	/** Null stays null: calc-core reads that as "no supply given" and leaves the verdict unanswered. */
	private static WaterSupply toCoreWaterSupply(WaterSupplyDto dto) {
		if (dto == null) {
			return null;
		}
		return switch (dto.source()) {
			case FLOW_TEST -> new SupplyCurve(
					dto.flowTest().staticPressurePsi(),
					dto.flowTest().residualPressurePsi(),
					dto.flowTest().flowAtResidualGpm());
			case GRAVITY_TANK -> new GravityTankSupply(
					dto.gravityTank().waterLevelHeightFt(),
					toCoreOutletRuns(dto.gravityTank().outletRuns()));
		};
	}

	private static List<SupplyPipeRun> toCoreOutletRuns(List<SupplyPipeRunDto> runs) {
		if (runs == null) {
			return List.of();
		}
		return runs.stream()
				.map(run -> new SupplyPipeRun(
						run.pipeMaterial(),
						run.schedule(),
						run.nominalSize(),
						run.cFactor(),
						run.pipeLengthFt(),
						toCoreFittings(run.fittings())))
				.toList();
	}

	private static List<DeviceLoss> toCoreDeviceLosses(List<DeviceLossDto> deviceLosses) {
		if (deviceLosses == null) {
			return List.of();
		}
		return deviceLosses.stream()
				.map(dto -> new DeviceLoss(dto.name(), dto.pressureLossPsi()))
				.toList();
	}

	private static SprinklerSystemType toCoreSystemType(SprinklerSystemTypeDto dto) {
		return switch (dto) {
			case WET -> SprinklerSystemType.WET;
			case DRY -> SprinklerSystemType.DRY;
			case PREACTION -> SprinklerSystemType.PREACTION;
		};
	}

	private static SprinklerHazard toCoreHazard(SprinklerHazardDto dto) {
		return switch (dto) {
			case LIGHT -> SprinklerHazard.LIGHT;
			case ORDINARY_GROUP_1 -> SprinklerHazard.ORDINARY_GROUP_1;
			case ORDINARY_GROUP_2 -> SprinklerHazard.ORDINARY_GROUP_2;
			case EXTRA_GROUP_1 -> SprinklerHazard.EXTRA_GROUP_1;
			case EXTRA_GROUP_2 -> SprinklerHazard.EXTRA_GROUP_2;
		};
	}

}
