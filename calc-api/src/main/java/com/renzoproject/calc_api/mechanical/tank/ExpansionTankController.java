package com.renzoproject.calc_api.mechanical.tank;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mechanical/tank/expansion")
public class ExpansionTankController {

	private final ExpansionTankService service;

	public ExpansionTankController(ExpansionTankService service) {
		this.service = service;
	}

	@PostMapping
	public ExpansionTankResponse calculate(@Valid @RequestBody ExpansionTankRequest request) {
		return service.calculate(request);
	}

}
