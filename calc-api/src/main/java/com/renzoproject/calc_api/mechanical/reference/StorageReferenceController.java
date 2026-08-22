package com.renzoproject.calc_api.mechanical.reference;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mechanical/reference")
public class StorageReferenceController {

	private final StorageReferenceService service;

	public StorageReferenceController(StorageReferenceService service) {
		this.service = service;
	}

	@GetMapping("/lpcd-consumption-table")
	public List<OccupancyTypeEntryDto> lpcdConsumptionTable() {
		return service.listLpcdConsumptionTable();
	}

	@GetMapping("/wsfu-demand-table")
	public List<WsfuDemandEntryDto> wsfuDemandTable() {
		return service.listWsfuDemandTable();
	}

	@GetMapping("/fire-water-duration-table")
	public List<FireWaterDurationEntryDto> fireWaterDurationTable() {
		return service.listFireWaterDurationTable();
	}

}
