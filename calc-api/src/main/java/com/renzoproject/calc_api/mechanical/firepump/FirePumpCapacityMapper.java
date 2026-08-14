package com.renzoproject.calc_api.mechanical.firepump;

import com.renzoproject.calc.core.mechanical.firepump.FirePumpUnits;
import com.renzoproject.calc.core.mechanical.firepump.StandardPumpRating;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;
import tech.units.indriya.quantity.Quantities;

import javax.measure.Quantity;

public final class FirePumpCapacityMapper {

	private FirePumpCapacityMapper() {
	}

	public static Quantity<VolumetricFlowRate> toCoreRatedFlow(FirePumpCapacityRequest request) {
		return Quantities.getQuantity(request.ratedFlowGpm(), FirePumpUnits.GPM);
	}

	public static FirePumpCapacityResponse toResponse(StandardPumpRating rating) {
		return new FirePumpCapacityResponse(rating.standardFlow().to(FirePumpUnits.GPM).getValue().doubleValue());
	}

}
