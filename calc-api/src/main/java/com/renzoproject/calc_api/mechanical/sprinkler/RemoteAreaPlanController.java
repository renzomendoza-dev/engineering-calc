package com.renzoproject.calc_api.mechanical.sprinkler;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mechanical/sprinkler/remote-area")
public class RemoteAreaPlanController {

	private final RemoteAreaPlanService service;

	public RemoteAreaPlanController(RemoteAreaPlanService service) {
		this.service = service;
	}

	@PostMapping
	public RemoteAreaPlanResponse plan(@Valid @RequestBody RemoteAreaPlanRequest request) {
		return service.plan(request);
	}

}
