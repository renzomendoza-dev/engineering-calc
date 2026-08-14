package com.renzoproject.calc_api.electrical.wiresizing;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/electrical/wire-sizing")
public class WireSizingController {

	private final WireSizingService service;

	public WireSizingController(WireSizingService service) {
		this.service = service;
	}

	@PostMapping
	public WireSizingResponse calculate(@Valid @RequestBody WireSizingRequest request) {
		return service.calculate(request);
	}

}
