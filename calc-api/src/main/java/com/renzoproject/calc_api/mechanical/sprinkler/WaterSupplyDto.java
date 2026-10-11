package com.renzoproject.calc_api.mechanical.sprinkler;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * Where the system's water comes from. {@code source} says which block below is filled in, and
 * exactly that one must be present -- the same mode-switch shape {@code VoltageDropRequest} uses,
 * rather than polymorphic JSON, so the body stays readable and the field names stay stable.
 *
 * <p>A source with no pressure of its own (a cistern or ground-level tank awaiting a pump) is not
 * a variant here: that system omits {@code waterSupply} from the request altogether, and the
 * response comes back with the demand figures and a null margin and verdict.
 *
 * @param source      which kind of source this is
 * @param flowTest    required when {@code source} is {@code FLOW_TEST}, omitted otherwise
 * @param gravityTank required when {@code source} is {@code GRAVITY_TANK}, omitted otherwise
 */
public record WaterSupplyDto(
		@NotNull WaterSupplySourceDto source,
		@Valid FlowTestSupplyDto flowTest,
		@Valid GravityTankSupplyDto gravityTank) {

	@AssertTrue(message = "Provide exactly the block matching 'source': flowTest for FLOW_TEST, "
			+ "gravityTank for GRAVITY_TANK")
	public boolean isSourceBlockPresent() {
		if (source == null) {
			return true; // @NotNull reports this; don't report it twice.
		}
		return switch (source) {
			case FLOW_TEST -> flowTest != null && gravityTank == null;
			case GRAVITY_TANK -> gravityTank != null && flowTest == null;
		};
	}

}
