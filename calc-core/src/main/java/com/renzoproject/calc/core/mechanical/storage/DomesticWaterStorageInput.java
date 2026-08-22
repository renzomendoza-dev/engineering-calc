package com.renzoproject.calc.core.mechanical.storage;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Input for {@link DomesticWaterStorageCalculator}. Which fields are required depends on
 * {@link #demandBasis()} -- see the per-field notes below.
 *
 * @param demandBasis           which demand model to use
 * @param occupantCount         required (and must be positive) for {@link DemandBasis#OCCUPANT_LOAD};
 *                               ignored otherwise
 * @param occupancyType         required for {@link DemandBasis#OCCUPANT_LOAD}, resolved via
 *                               {@link PerCapitaConsumptionResolver}; ignored otherwise
 * @param totalFixtureUnits     required (and must be positive) for {@link DemandBasis#FIXTURE_UNIT};
 *                               ignored otherwise
 * @param systemType            required for {@link DemandBasis#FIXTURE_UNIT}; ignored otherwise
 * @param storageDurationHours  must be positive
 * @param safetyMarginPercent   applied as {@code (1 + safetyMarginPercent/100)} to the final
 *                               volume; pass {@code 0} for no margin
 * @throws CalculationException if any of the above rules is violated
 */
public record DomesticWaterStorageInput(
		DemandBasis demandBasis,
		Integer occupantCount,
		String occupancyType,
		Double totalFixtureUnits,
		SystemType systemType,
		double storageDurationHours,
		double safetyMarginPercent) {

	public DomesticWaterStorageInput {
		if (demandBasis == null) {
			throw new CalculationException("demandBasis is required");
		}
		if (storageDurationHours <= 0) {
			throw new CalculationException("storageDurationHours must be positive");
		}
		if (demandBasis == DemandBasis.OCCUPANT_LOAD) {
			if (occupantCount == null || occupantCount <= 0) {
				throw new CalculationException("occupantCount must be positive for OCCUPANT_LOAD demand basis");
			}
			if (occupancyType == null || occupancyType.isBlank()) {
				throw new CalculationException("occupancyType is required for OCCUPANT_LOAD demand basis");
			}
		} else {
			if (totalFixtureUnits == null || totalFixtureUnits <= 0) {
				throw new CalculationException("totalFixtureUnits must be positive for FIXTURE_UNIT demand basis");
			}
			if (systemType == null) {
				throw new CalculationException("systemType is required for FIXTURE_UNIT demand basis");
			}
		}
	}

}
