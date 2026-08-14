package com.renzoproject.calc_api.mechanical.reference;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mechanical/reference")
public class PipeReferenceController {

	private final PipeReferenceService service;

	public PipeReferenceController(PipeReferenceService service) {
		this.service = service;
	}

	@GetMapping("/pipe-materials")
	public List<PipeMaterialDto> pipeMaterials() {
		return service.listPipeMaterials();
	}

}
