package com.renzoproject.calc_api.mechanical.firepump;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mechanical/firepump/power")
public class FirePumpPowerController {

	private final FirePumpPowerService service;

	public FirePumpPowerController(FirePumpPowerService service) {
		this.service = service;
	}

	@PostMapping
	public FirePumpPowerResponse calculate(@Valid @RequestBody FirePumpPowerRequest request) {
		return service.calculate(request);
	}

}
