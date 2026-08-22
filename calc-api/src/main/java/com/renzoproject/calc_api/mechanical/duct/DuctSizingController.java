package com.renzoproject.calc_api.mechanical.duct;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mechanical/duct/sizing")
public class DuctSizingController {

	private final DuctSizingService service;

	public DuctSizingController(DuctSizingService service) {
		this.service = service;
	}

	@PostMapping
	public DuctSizingResponse calculate(@Valid @RequestBody DuctSizingRequest request) {
		return service.calculate(request);
	}

}
