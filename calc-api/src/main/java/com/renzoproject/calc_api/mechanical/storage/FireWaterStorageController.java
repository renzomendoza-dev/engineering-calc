package com.renzoproject.calc_api.mechanical.storage;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mechanical/storage/fire")
public class FireWaterStorageController {

	private final FireWaterStorageService service;

	public FireWaterStorageController(FireWaterStorageService service) {
		this.service = service;
	}

	@PostMapping
	public FireWaterStorageResponse calculate(@Valid @RequestBody FireWaterStorageRequest request) {
		return service.calculate(request);
	}

}
