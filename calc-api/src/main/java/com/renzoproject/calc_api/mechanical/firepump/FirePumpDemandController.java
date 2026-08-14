package com.renzoproject.calc_api.mechanical.firepump;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mechanical/firepump/demand")
public class FirePumpDemandController {

	private final FirePumpDemandService service;

	public FirePumpDemandController(FirePumpDemandService service) {
		this.service = service;
	}

	@PostMapping
	public FirePumpDemandResponse calculate(@Valid @RequestBody FirePumpDemandRequest request) {
		return service.calculate(request);
	}

}
