package com.renzoproject.calc_api.electrical.powerconsumption;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/electrical/power-consumption")
public class PowerConsumptionController {

	private final PowerConsumptionService service;

	public PowerConsumptionController(PowerConsumptionService service) {
		this.service = service;
	}

	@PostMapping
	public PowerConsumptionResponse calculate(@Valid @RequestBody PowerConsumptionRequest request) {
		return service.calculate(request);
	}

}
