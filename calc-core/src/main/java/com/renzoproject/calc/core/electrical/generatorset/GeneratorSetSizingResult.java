package com.renzoproject.calc.core.electrical.generatorset;

import com.renzoproject.calc.core.electrical.wiresizing.WireSizingResult;

import java.util.List;

/**
 * @param loadRows                         every load with its demand and the rule it came from
 * @param connectedKva                     sum of the connected loads
 * @param demandKva                        sum of the loads after the Article 2.20 demand factors, motors at
 *                                          full-load current
 * @param demandCurrentAmps                {@code demandKva} as current at the system voltage
 * @param spareCapacityPercent             echoed from the input
 * @param requiredKva                      {@code demandKva} plus the spare capacity
 * @param requiredKw                       {@code requiredKva} at the generator power factor
 * @param generatorPowerFactor             echoed from the input
 * @param loadFeederMinimumAmpacityAmps    ampacity of the conductors from the transfer equipment to the loads
 *                                          per 4.30.2.4; {@code null} when there is no motor
 * @param loadFeederBasis                  how that ampacity was built; {@code null} when there is no motor
 * @param recommendedStandardKva           the smallest standard size at or above {@code requiredKva};
 *                                          {@code null} when the list has none large enough
 * @param selectedGeneratorKva             the set the output circuit is checked against: the one entered, or
 *                                          the recommended size; {@code null} when neither exists
 * @param selectedIsUserEntered            whether {@code selectedGeneratorKva} came from the input
 * @param selectedMeetsRequired            whether the selected set covers {@code requiredKva}; {@code null}
 *                                          when there is no selected set
 * @param loadPercentOfSelected            {@code demandKva} as a percent of the selected set; {@code null}
 *                                          when there is no selected set
 * @param nameplateCurrentAmps             the selected set's rated current at the system voltage
 * @param outputConductorFactor            1.15, or 1.00 where the design prevents overload (4.45.1.13)
 * @param minimumOutputConductorAmpacityAmps nameplate current times that factor
 * @param outputConductor                  the conductor sized for that ampacity; {@code null} when no
 *                                          conductor conditions were given
 * @param capacityCitation                 the Chapter 7 section that sets the capacity rule
 * @param capacityBasis                    that rule in words
 * @param requirements                     Chapter 7 requirements for this standby class, to check off
 * @param standardSizesNote                where the standard sizes come from (they are not PEC data)
 * @param warnings                         anything that needs the caller's attention
 */
public record GeneratorSetSizingResult(
		List<LoadRow> loadRows,
		double connectedKva,
		double demandKva,
		double demandCurrentAmps,
		double spareCapacityPercent,
		double requiredKva,
		double requiredKw,
		double generatorPowerFactor,
		Double loadFeederMinimumAmpacityAmps,
		String loadFeederBasis,
		Double recommendedStandardKva,
		Double selectedGeneratorKva,
		boolean selectedIsUserEntered,
		Boolean selectedMeetsRequired,
		Double loadPercentOfSelected,
		Double nameplateCurrentAmps,
		double outputConductorFactor,
		Double minimumOutputConductorAmpacityAmps,
		WireSizingResult outputConductor,
		String capacityCitation,
		String capacityBasis,
		List<Requirement> requirements,
		String standardSizesNote,
		List<String> warnings) {

	/**
	 * @param kind                load type
	 * @param label               name from the input, with the quantity for motors
	 * @param connectedVoltAmperes load before any demand factor
	 * @param demandFactorPercent the factor actually applied (the effective one where a stepped table was used)
	 * @param demandVoltAmperes   load after the demand factor
	 * @param continuous          whether the load runs three hours or more
	 * @param basis               the PEC sections and figures this row came from
	 */
	public record LoadRow(
			LoadKind kind,
			String label,
			double connectedVoltAmperes,
			double demandFactorPercent,
			double demandVoltAmperes,
			boolean continuous,
			String basis) {
	}

	public record Requirement(String citation, String text) {
	}

}
