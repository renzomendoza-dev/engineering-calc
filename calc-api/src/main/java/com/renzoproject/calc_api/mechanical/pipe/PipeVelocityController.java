package com.renzoproject.calc_api.mechanical.pipe;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mechanical/pipe-velocity")
public class PipeVelocityController {

	private final PipeVelocityService service;

	public PipeVelocityController(PipeVelocityService service) {
		this.service = service;
	}

	@PostMapping
	public PipeVelocityResponse calculate(@Valid @RequestBody PipeVelocityRequest request) {
		return service.calculate(request);
	}

}
