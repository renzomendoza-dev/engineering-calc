package com.renzoproject.calc_api.mechanical.firepump;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mechanical/firepump/capacity")
public class FirePumpCapacityController {

	private final FirePumpCapacityService service;

	public FirePumpCapacityController(FirePumpCapacityService service) {
		this.service = service;
	}

	@PostMapping
	public FirePumpCapacityResponse resolve(@Valid @RequestBody FirePumpCapacityRequest request) {
		return service.resolve(request);
	}

}
