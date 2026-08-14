package com.renzoproject.calc_api.electrical.voltagedrop;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/electrical/voltage-drop")
public class VoltageDropController {

	private final VoltageDropService service;

	public VoltageDropController(VoltageDropService service) {
		this.service = service;
	}

	@PostMapping
	public VoltageDropResponse calculate(@Valid @RequestBody VoltageDropRequest request) {
		return service.calculate(request);
	}

}
