package com.renzoproject.calc_api.electrical.motorflc;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/electrical/motor-conductor-sizing")
public class MotorConductorSizingController {

	private final MotorConductorSizingService service;

	public MotorConductorSizingController(MotorConductorSizingService service) {
		this.service = service;
	}

	@PostMapping
	public MotorConductorSizingResponse calculate(@Valid @RequestBody MotorConductorSizingRequest request) {
		return service.calculate(request);
	}

}
