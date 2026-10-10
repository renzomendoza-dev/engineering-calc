package com.renzoproject.calc_api.mechanical.reference;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mechanical/reference")
public class SprinklerReferenceController {

	private final SprinklerReferenceService service;

	public SprinklerReferenceController(SprinklerReferenceService service) {
		this.service = service;
	}

	@GetMapping("/sprinkler-hazard-presets-table")
	public List<HazardPresetEntryDto> sprinklerHazardPresetsTable() {
		return service.listHazardPresets();
	}

	@GetMapping("/sprinkler-c-factors-table")
	public List<SprinklerCFactorEntryDto> sprinklerCFactorsTable() {
		return service.listCFactors();
	}

	@GetMapping("/sprinkler-fitting-equivalent-lengths-table")
	public List<FittingEquivalentLengthEntryDto> sprinklerFittingEquivalentLengthsTable() {
		return service.listFittingEquivalentLengths();
	}

	@GetMapping("/sprinkler-k-factors-table")
	public List<SprinklerKFactorEntryDto> sprinklerKFactorsTable() {
		return service.listKFactors();
	}

}
