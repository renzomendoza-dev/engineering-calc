package com.renzoproject.calc_api.electrical.generatorset;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/electrical/generator-set-sizing")
public class GeneratorSetSizingController {

	private final GeneratorSetSizingService service;
	private final GeneratorSetReferenceService referenceService;

	public GeneratorSetSizingController(GeneratorSetSizingService service, GeneratorSetReferenceService referenceService) {
		this.service = service;
		this.referenceService = referenceService;
	}

	@PostMapping
	public GeneratorSetSizingResponse calculate(@Valid @RequestBody GeneratorSetSizingRequest request) {
		return service.calculate(request);
	}

	/** What the form offers: occupancies, area types, standby classes, transfer modes and the standard sizes. */
	@GetMapping("/reference")
	public GeneratorSetReferenceResponse reference() {
		return referenceService.reference();
	}

}
