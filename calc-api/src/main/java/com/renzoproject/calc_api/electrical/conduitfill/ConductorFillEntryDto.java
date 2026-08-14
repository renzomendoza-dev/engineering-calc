package com.renzoproject.calc_api.electrical.conduitfill;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * One conductor entry in a conduit fill request.
 *
 * <p>{@code insulationType} is accepted as a plain string and mapped to calc-core's
 * {@code InsulationType} enum explicitly in {@link ConduitFillMapper}, rather than binding
 * Jackson directly to the enum. calc-core's {@code InsulationType} has 49 values with labels
 * like {@code "RFH-2"} that aren't valid Java identifiers as-is; a bad value here should
 * surface as a clear "Unknown insulation type: X" {@code CalculationException} → 400, not a
 * generic Jackson deserialization error.
 */
public record ConductorFillEntryDto(
		@NotBlank String insulationType,
		@NotBlank String sizeLabel,
		@Min(1) int quantity) {

}
