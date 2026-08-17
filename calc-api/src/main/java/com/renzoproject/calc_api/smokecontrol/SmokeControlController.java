package com.renzoproject.calc_api.smokecontrol;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Smoke control domain endpoints. Named after the domain (not the single plume feature) since
 * this is expected to host further smoke-control endpoints later (vent area sizing, make-up air
 * — both explicitly out of scope for now), the same multi-endpoint-per-domain-controller shape
 * already used by {@code ConductorReferenceController}.
 */
@RestController
@RequestMapping("/api/smoke-control")
public class SmokeControlController {

	private final SmokeProductionService smokeProductionService;
	private final TSquaredSmokeProductionService tSquaredSmokeProductionService;
	private final VentAreaService ventAreaService;

	public SmokeControlController(
			SmokeProductionService smokeProductionService,
			TSquaredSmokeProductionService tSquaredSmokeProductionService,
			VentAreaService ventAreaService) {
		this.smokeProductionService = smokeProductionService;
		this.tSquaredSmokeProductionService = tSquaredSmokeProductionService;
		this.ventAreaService = ventAreaService;
	}

	@PostMapping("/plume")
	public SmokeProductionResponse calculatePlume(@Valid @RequestBody SmokeProductionRequest request) {
		return smokeProductionService.calculate(request);
	}

	@PostMapping("/plume-tsquared")
	public TSquaredSmokeProductionResponse calculatePlumeTSquared(@Valid @RequestBody TSquaredSmokeProductionRequest request) {
		return tSquaredSmokeProductionService.calculate(request);
	}

	@PostMapping("/vent-area")
	public VentAreaResponse calculateVentArea(@Valid @RequestBody VentAreaRequest request) {
		return ventAreaService.calculate(request);
	}

}
