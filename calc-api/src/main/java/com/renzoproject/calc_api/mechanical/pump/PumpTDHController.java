package com.renzoproject.calc_api.mechanical.pump;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mechanical/pump/tdh")
public class PumpTDHController {

	private final PumpTDHService service;

	public PumpTDHController(PumpTDHService service) {
		this.service = service;
	}

	@PostMapping
	public PumpTDHResponse calculateTdh(@Valid @RequestBody PumpTDHRequest request) {
		return service.calculate(request);
	}

}
