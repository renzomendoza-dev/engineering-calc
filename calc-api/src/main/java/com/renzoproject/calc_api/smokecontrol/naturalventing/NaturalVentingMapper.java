package com.renzoproject.calc_api.smokecontrol.naturalventing;

import com.renzoproject.calc.core.smokecontrol.naturalventing.FireCase;
import com.renzoproject.calc.core.smokecontrol.naturalventing.FireCaseKind;
import com.renzoproject.calc.core.smokecontrol.naturalventing.NaturalVentingInput;
import com.renzoproject.calc.core.smokecontrol.naturalventing.NaturalVentingResult;
import com.renzoproject.calc.core.smokecontrol.naturalventing.NaturalVentingResult.FireCaseResult;
import com.renzoproject.calc.core.smokecontrol.naturalventing.NaturalVentingResult.ZoneResult;
import com.renzoproject.calc.core.smokecontrol.naturalventing.VentZone;
import com.renzoproject.calc_api.common.EnumParsing;

/**
 * Pure mapping between the request/response DTOs and calc-core, no logic.
 */
public final class NaturalVentingMapper {

	private NaturalVentingMapper() {
	}

	public static NaturalVentingInput toCoreInput(NaturalVentingRequest request) {
		return new NaturalVentingInput(
				request.ambientTemperature(),
				request.ambientDensity(),
				request.specificHeat(),
				request.layerEnergyFraction(),
				request.growthConstant(),
				request.zones().stream().map(NaturalVentingMapper::toCoreZone).toList(),
				request.fireCases().stream().map(NaturalVentingMapper::toCoreFireCase).toList());
	}

	private static VentZone toCoreZone(VentZoneDto zone) {
		return new VentZone(
				zone.name(), zone.ceilingHeight(), zone.ventCentreHeight(), zone.curtainDepth(), zone.zoneLength(), zone.zoneWidth(),
				zone.ventDischargeCoefficient(), zone.ventCount(), zone.ventTotalArea(), zone.ventUnitLength(), zone.ventUnitWidth(),
				zone.ventSpacing(), zone.ventToWallDistance(), zone.inletDischargeCoefficient(), zone.inletAreaRatio(),
				zone.inletNetArea(), zone.maxLayerTemperatureRise(), zone.maxFloorRadiantFlux(), zone.aisleWidth());
	}

	private static FireCase toCoreFireCase(FireCaseDto fireCase) {
		return new FireCase(
				fireCase.name(), fireCase.zoneName(), EnumParsing.parse(FireCaseKind.class, fireCase.kind(), "fire case kind"),
				fireCase.steadyHeatReleaseRate(), fireCase.growthTime(), fireCase.detectionTime(), fireCase.designInterval(),
				fireCase.maxHeatReleaseRate(), fireCase.fireDiameter(), fireCase.fireArea(), fireCase.unitHeatReleaseRate(),
				fireCase.largestFireDimension(), fireCase.fireBaseHeight(), fireCase.smokeLayerHeight());
	}

	public static NaturalVentingResponse toResponse(NaturalVentingResult result) {
		return new NaturalVentingResponse(
				result.fireCases().stream().map(NaturalVentingMapper::toDto).toList(),
				result.zones().stream().map(NaturalVentingMapper::toDto).toList());
	}

	private static FireCaseResultDto toDto(FireCaseResult r) {
		return new FireCaseResultDto(
				r.name(), r.zoneName(), r.designHeatReleaseRate(), r.heatReleaseRateCapped(), r.growthCoefficient(), r.fireDiameter(),
				r.meanFlameHeight(), r.virtualOrigin(), r.layerHeightAboveFire(), r.layerDepth(), r.regime().name(), r.lineFireFactor(),
				r.convectiveHeatReleaseRate(), r.plumeMassFlow(), r.layerTemperatureRise(), r.layerTemperature(), r.layerDensity(),
				r.aerodynamicVentArea(), r.requiredVentArea(), r.inletCorrection(), r.requiredInletArea(), r.inletAreaForVelocityLimit(),
				r.inletVelocity(), r.floorRadiantFlux(), r.feasibleHeatReleaseRate(), r.minimumAisleWidth(),
				r.checks().stream().map(VentingCheckDto::from).toList());
	}

	private static ZoneResultDto toDto(ZoneResult zone) {
		return new ZoneResultDto(zone.name(), zone.checks().stream().map(VentingCheckDto::from).toList());
	}

}
