package com.renzoproject.calc_api.mechanical.reference;

import com.renzoproject.calc.core.mechanical.sprinkler.FittingEquivalentLengthRow;

import java.util.Map;

/**
 * One fitting type's published equivalent lengths, keyed by nominal size.
 *
 * <p>{@code equivalentLengthFtByNominalSize} is empty while the published table has not been
 * entered -- a caller should treat an empty map as "this fitting cannot be looked up yet" and
 * either collect an explicit equivalent length from the user or hide the fitting.
 */
public record FittingEquivalentLengthEntryDto(
		String fittingType,
		String label,
		String confidence,
		Map<String, Double> equivalentLengthFtByNominalSize) {

	public static FittingEquivalentLengthEntryDto from(FittingEquivalentLengthRow row) {
		return new FittingEquivalentLengthEntryDto(
				row.fittingType(),
				row.label(),
				row.confidence(),
				row.equivalentLengthFtByNominalSize());
	}

}
