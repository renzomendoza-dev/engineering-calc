package com.renzoproject.calc_api.electrical.generatorset;

import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingResult.FirePumpConductor;

/**
 * The conductors to a fire pump controller, mirroring calc-core's
 * {@code GeneratorSetSizingResult.FirePumpConductor}. <b>Units:</b> mm2 (the size labels), amperes.
 *
 * @param sizeLabel                  the size chosen, one conductor of {@code numberOfParallelSets}
 * @param ampacitySizeLabel          the smallest size that carries the required ampacity alone
 * @param upsizedForVoltageDrop      whether {@code sizeLabel} is larger than {@code ampacitySizeLabel} because
 *                                   of the voltage drop limits
 * @param numberOfParallelSets       identical conductors in parallel
 * @param requiredAmpacityPerSetAmps what each conductor must carry
 * @param deratedAmpacityAmps        one conductor's ampacity after temperature and conductor count
 * @param meetsTerminationRating     whether it also satisfies the termination temperature rating
 */
public record FirePumpConductorDto(
		String sizeLabel,
		String ampacitySizeLabel,
		boolean upsizedForVoltageDrop,
		int numberOfParallelSets,
		double requiredAmpacityPerSetAmps,
		double deratedAmpacityAmps,
		boolean meetsTerminationRating) {

	public static FirePumpConductorDto from(FirePumpConductor conductor) {
		return new FirePumpConductorDto(
				conductor.sizeLabel(), conductor.ampacitySizeLabel(), conductor.upsizedForVoltageDrop(),
				conductor.numberOfParallelSets(), conductor.requiredAmpacityPerSetAmps(),
				conductor.deratedAmpacityAmps(), conductor.meetsTerminationRating());
	}

}
