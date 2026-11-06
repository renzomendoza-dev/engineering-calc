package com.renzoproject.calc_api.mechanical.standpipe;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mechanical/standpipe")
public class StandpipeHydraulicController {

	private final StandpipeHydraulicService service;

	public StandpipeHydraulicController(StandpipeHydraulicService service) {
		this.service = service;
	}

	@PostMapping("/hydraulics")
	public StandpipeHydraulicResponse calculate(@Valid @RequestBody StandpipeHydraulicRequest request) {
		return service.calculate(request);
	}

	/** The equivalent length chart and C table the calculation uses, with the edition they come from. */
	@GetMapping("/reference")
	public StandpipeReferenceResponse reference() {
		return service.reference();
	}

}
