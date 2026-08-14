package com.renzoproject.calc_api.electrical.motorflc;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/electrical/locked-rotor")
public class LockedRotorController {

	private final LockedRotorService service;

	public LockedRotorController(LockedRotorService service) {
		this.service = service;
	}

	@PostMapping
	public LockedRotorResponse calculate(@Valid @RequestBody LockedRotorRequest request) {
		return service.calculate(request);
	}

}
