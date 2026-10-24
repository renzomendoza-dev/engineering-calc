package com.renzoproject.calc_api.electrical.generatorset;

import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetReference;
import org.springframework.stereotype.Service;

/**
 * Reads calc-core's generator set reference data for the form, same pattern as the other
 * reference services.
 */
@Service
public class GeneratorSetReferenceService {

	private final GeneratorSetReference reference = new GeneratorSetReference();

	public GeneratorSetReferenceResponse reference() {
		var fullDemandGroups = reference.demandGroups().stream()
				.filter(GeneratorSetReference.DemandGroup::fullDemandWhenAllLightingInUse)
				.map(GeneratorSetReference.DemandGroup::key)
				.toList();
		return new GeneratorSetReferenceResponse(
				reference.occupancies().stream()
						.map(occupancy -> new LightingOccupancyDto(
								occupancy.key(), occupancy.label(), occupancy.voltAmperesPerM2(),
								fullDemandGroups.contains(occupancy.demandGroup()), occupancy.note()))
						.toList(),
				reference.areaTypes().stream()
						.map(areaType -> new LightingAreaTypeDto(areaType.key(), areaType.label(), areaType.voltAmperesPerM2()))
						.toList(),
				reference.standbyClasses().stream()
						.map(entry -> new StandbyClassOptionDto(entry.key(), entry.label(), entry.capacityCitation()))
						.toList(),
				reference.optionalTransfers().stream()
						.map(entry -> new OptionalTransferOptionDto(entry.key(), entry.label(), entry.citation(), entry.text()))
						.toList(),
				reference.standardSizesKva(),
				reference.standardSizesNote(),
				reference.defaultGeneratorPowerFactor(),
				reference.defaultGeneratorPowerFactorNote(),
				reference.defaultSubtransientReactancePercent(),
				reference.defaultMaximumVoltageDipPercent(),
				reference.motorStartingDefaultsNote(),
				reference.defaultFirePumpStartingPowerFactor(),
				reference.defaultFirePumpRunningPowerFactor(),
				reference.defaultCableStartingDropAllowancePercent(),
				reference.firePumpDefaultsNote());
	}

}
