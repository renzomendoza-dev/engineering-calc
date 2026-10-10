package com.renzoproject.calc_api.mechanical.sprinkler;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mechanical/sprinkler/hydraulics")
public class SprinklerHydraulicController {

	private final SprinklerHydraulicService service;

	public SprinklerHydraulicController(SprinklerHydraulicService service) {
		this.service = service;
	}

	@PostMapping
	public SprinklerHydraulicResponse calculate(@Valid @RequestBody SprinklerHydraulicRequest request) {
		return service.calculate(request);
	}

}
