package com.renzoproject.calc_api.mechanical.tank;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mechanical/tank/pressure")
public class PressureTankController {

	private final PressureTankService service;

	public PressureTankController(PressureTankService service) {
		this.service = service;
	}

	@PostMapping
	public PressureTankResponse calculate(@Valid @RequestBody PressureTankRequest request) {
		return service.calculate(request);
	}

}
