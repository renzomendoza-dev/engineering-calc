package com.renzoproject.calc_api.acoustics;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/acoustics/distance-attenuation")
public class DistanceAttenuationController {

	private final DistanceAttenuationService service;

	public DistanceAttenuationController(DistanceAttenuationService service) {
		this.service = service;
	}

	@PostMapping
	public DistanceAttenuationResponse calculate(@Valid @RequestBody DistanceAttenuationRequest request) {
		return service.calculate(request);
	}

}
