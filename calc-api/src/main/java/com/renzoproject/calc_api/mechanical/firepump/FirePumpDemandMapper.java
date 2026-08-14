package com.renzoproject.calc_api.mechanical.firepump;

import com.renzoproject.calc.core.mechanical.firepump.FirePumpDemandInput;
import com.renzoproject.calc.core.mechanical.firepump.FirePumpDemandResult;
import com.renzoproject.calc.core.mechanical.firepump.FirePumpUnits;
import com.renzoproject.calc.core.mechanical.firepump.SuctionCondition;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;
import tech.units.indriya.quantity.Quantities;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;

public final class FirePumpDemandMapper {

	private FirePumpDemandMapper() {
	}

	public static FirePumpDemandInput toCoreInput(FirePumpDemandRequest request) {
		Quantity<VolumetricFlowRate> requiredFlow = Quantities.getQuantity(request.requiredFlowGpm(), FirePumpUnits.GPM);
		Quantity<Pressure> requiredPressure = Quantities.getQuantity(request.requiredPressurePsiAtDemandPoint(), FirePumpUnits.PSI);
		SuctionCondition suctionCondition = toCoreSuctionCondition(request.suctionCondition());

		Quantity<Pressure> availableSuctionPressure = request.availableSuctionPressurePsi() == null
				? null
				: Quantities.getQuantity(request.availableSuctionPressurePsi(), FirePumpUnits.PSI);
		Quantity<Length> suctionLift = request.suctionLiftFeet() == null
				? null
				: Quantities.getQuantity(request.suctionLiftFeet(), FirePumpUnits.FOOT);
		double safetyMarginPercent = request.safetyMarginPercent() == null ? 0.0 : request.safetyMarginPercent();

		return new FirePumpDemandInput(requiredFlow, requiredPressure, suctionCondition, availableSuctionPressure, suctionLift, safetyMarginPercent);
	}

	public static FirePumpDemandResponse toResponse(FirePumpDemandResult result) {
		return new FirePumpDemandResponse(
				result.ratedFlow().to(FirePumpUnits.GPM).getValue().doubleValue(),
				result.ratedPressure().to(FirePumpUnits.PSI).getValue().doubleValue());
	}

	private static SuctionCondition toCoreSuctionCondition(SuctionConditionDto dto) {
		return switch (dto) {
			case FLOODED -> SuctionCondition.FLOODED;
			case LIFT -> SuctionCondition.LIFT;
		};
	}

}
