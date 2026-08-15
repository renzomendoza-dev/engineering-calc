package com.renzoproject.calc_api.mechanical.pump;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mechanical/pump/power")
public class PumpPowerController {

	private final PumpPowerService service;

	public PumpPowerController(PumpPowerService service) {
		this.service = service;
	}

	@PostMapping
	public PumpPowerResponse calculatePower(@Valid @RequestBody PumpPowerRequest request) {
		return service.calculate(request);
	}

}
