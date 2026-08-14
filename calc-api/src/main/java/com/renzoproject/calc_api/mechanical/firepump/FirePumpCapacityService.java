package com.renzoproject.calc_api.mechanical.firepump;

import com.renzoproject.calc.core.mechanical.firepump.FirePumpCapacityResolver;
import com.renzoproject.calc.core.mechanical.firepump.JsonFirePumpCapacityResolver;
import org.springframework.stereotype.Service;

/**
 * Thin pass-through to {@link FirePumpCapacityResolver} — simpler than the other three
 * services, since there's no calculator here, just a lookup.
 */
@Service
public class FirePumpCapacityService {

	private final FirePumpCapacityResolver resolver = new JsonFirePumpCapacityResolver();

	public FirePumpCapacityResponse resolve(FirePumpCapacityRequest request) {
		var ratedFlow = FirePumpCapacityMapper.toCoreRatedFlow(request);
		var rating = resolver.resolveNextStandardCapacity(ratedFlow);
		return FirePumpCapacityMapper.toResponse(rating);
	}

}
