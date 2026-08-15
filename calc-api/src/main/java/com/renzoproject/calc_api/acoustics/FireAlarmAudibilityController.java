package com.renzoproject.calc_api.acoustics;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/acoustics/fire-alarm-audibility")
public class FireAlarmAudibilityController {

	private final FireAlarmAudibilityService service;

	public FireAlarmAudibilityController(FireAlarmAudibilityService service) {
		this.service = service;
	}

	@PostMapping
	public FireAlarmAudibilityResponse calculate(@Valid @RequestBody FireAlarmAudibilityRequest request) {
		return service.calculate(request);
	}

}
