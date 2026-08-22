package com.renzoproject.calc_api.mechanical.storage;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mechanical/storage/domestic")
public class DomesticWaterStorageController {

	private final DomesticWaterStorageService service;

	public DomesticWaterStorageController(DomesticWaterStorageService service) {
		this.service = service;
	}

	@PostMapping
	public DomesticWaterStorageResponse calculate(@Valid @RequestBody DomesticWaterStorageRequest request) {
		return service.calculate(request);
	}

}
