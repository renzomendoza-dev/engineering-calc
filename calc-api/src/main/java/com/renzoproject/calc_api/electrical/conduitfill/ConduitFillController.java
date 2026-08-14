package com.renzoproject.calc_api.electrical.conduitfill;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/electrical/conduit-fill")
public class ConduitFillController {

	private final ConduitFillService service;

	public ConduitFillController(ConduitFillService service) {
		this.service = service;
	}

	@PostMapping
	public ConduitFillResponse calculate(@Valid @RequestBody ConduitFillRequest request) {
		return service.calculate(request);
	}

}
