package com.renzoproject.calc_api.mechanical.reference;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mechanical/reference")
public class DuctReferenceController {

	private final DuctReferenceService service;

	public DuctReferenceController(DuctReferenceService service) {
		this.service = service;
	}

	@GetMapping("/duct-velocity-limits-table")
	public List<DuctVelocityLimitEntryDto> ductVelocityLimitsTable() {
		return service.listVelocityLimits();
	}

	@GetMapping("/duct-roughness-table")
	public List<DuctRoughnessEntryDto> ductRoughnessTable() {
		return service.listDuctRoughnessTable();
	}

}
