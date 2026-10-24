package com.renzoproject.calc_api.electrical.generatorset;

import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingResult;
import com.renzoproject.calc_api.electrical.wiresizing.WireSizingResponse;

import java.util.List;

/**
 * HTTP response body for generator set sizing, mirroring calc-core's {@link GeneratorSetSizingResult}.
 * <b>Units:</b> kVA, kW, amperes; the load rows are in volt-amperes.
 *
 * <p>Reuses {@link WireSizingResponse} for the output conductor rather than redefining it.
 */
public record GeneratorSetSizingResponse(
		List<LoadRowDto> loadRows,
		double connectedKva,
		double demandKva,
		double demandCurrentAmps,
		double spareCapacityPercent,
		double requiredKva,
		double requiredKw,
		double generatorPowerFactor,
		Double loadFeederMinimumAmpacityAmps,
		String loadFeederBasis,
		Double recommendedStandardKva,
		Double selectedGeneratorKva,
		boolean selectedIsUserEntered,
		Boolean selectedMeetsRequired,
		Double loadPercentOfSelected,
		Double nameplateCurrentAmps,
		double outputConductorFactor,
		Double minimumOutputConductorAmpacityAmps,
		WireSizingResponse outputConductor,
		String capacityCitation,
		String capacityBasis,
		List<RequirementDto> requirements,
		String standardSizesNote,
		List<String> warnings,
		double runningRequiredKva,
		Double startingRequiredKva,
		String governingCase,
		List<StartingStepDto> startingSteps,
		String startingBasis) {

	public static GeneratorSetSizingResponse from(GeneratorSetSizingResult result) {
		return new GeneratorSetSizingResponse(
				result.loadRows().stream().map(LoadRowDto::from).toList(),
				result.connectedKva(),
				result.demandKva(),
				result.demandCurrentAmps(),
				result.spareCapacityPercent(),
				result.requiredKva(),
				result.requiredKw(),
				result.generatorPowerFactor(),
				result.loadFeederMinimumAmpacityAmps(),
				result.loadFeederBasis(),
				result.recommendedStandardKva(),
				result.selectedGeneratorKva(),
				result.selectedIsUserEntered(),
				result.selectedMeetsRequired(),
				result.loadPercentOfSelected(),
				result.nameplateCurrentAmps(),
				result.outputConductorFactor(),
				result.minimumOutputConductorAmpacityAmps(),
				result.outputConductor() == null ? null : WireSizingResponse.from(result.outputConductor()),
				result.capacityCitation(),
				result.capacityBasis(),
				result.requirements().stream()
						.map(requirement -> new RequirementDto(requirement.citation(), requirement.text()))
						.toList(),
				result.standardSizesNote(),
				result.warnings(),
				result.runningRequiredKva(),
				result.startingRequiredKva(),
				result.governingCase(),
				result.startingSteps().stream().map(StartingStepDto::from).toList(),
				result.startingBasis());
	}

}
