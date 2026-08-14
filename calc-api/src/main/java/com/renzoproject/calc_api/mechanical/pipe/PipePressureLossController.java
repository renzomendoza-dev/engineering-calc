package com.renzoproject.calc_api.mechanical.pipe;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mechanical/pipe-pressure-loss")
public class PipePressureLossController {

	private final PipePressureLossService service;

	public PipePressureLossController(PipePressureLossService service) {
		this.service = service;
	}

	@PostMapping
	public PipePressureLossResponse calculate(@Valid @RequestBody PipePressureLossRequest request) {
		return service.calculate(request);
	}

}
