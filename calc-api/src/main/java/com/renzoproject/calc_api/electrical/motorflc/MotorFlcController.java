package com.renzoproject.calc_api.electrical.motorflc;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/electrical/motor-flc")
public class MotorFlcController {

	private final MotorFlcService service;

	public MotorFlcController(MotorFlcService service) {
		this.service = service;
	}

	@PostMapping
	public MotorFlcResponse calculate(@Valid @RequestBody MotorFlcRequest request) {
		return service.calculate(request);
	}

}
