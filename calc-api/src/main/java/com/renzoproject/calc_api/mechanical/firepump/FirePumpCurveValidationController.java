package com.renzoproject.calc_api.mechanical.firepump;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mechanical/firepump/curve-validation")
public class FirePumpCurveValidationController {

	private final FirePumpCurveValidationService service;

	public FirePumpCurveValidationController(FirePumpCurveValidationService service) {
		this.service = service;
	}

	@PostMapping
	public FirePumpCurveValidationResponse validate(@Valid @RequestBody FirePumpCurveValidationRequest request) {
		return service.validate(request);
	}

}
