package com.renzoproject.calc_api.electrical.generatorset;

import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingResult.StartingStep;

/**
 * One step of motor starting, mirroring calc-core's {@code GeneratorSetSizingResult.StartingStep}.
 * <b>Units:</b> kVA, percent.
 *
 * @param step                        the step number from the request
 * @param motors                      the motors that start in it, with quantities
 * @param startingKva                 their locked-rotor kVA, after any starting current percentage
 * @param runningKvaBefore            the load already running when the step starts: every non-motor load and
 *                                    the motors of the earlier steps, at demand
 * @param requiredGeneratorKva        the set that holds the voltage dip within the limit for this step
 * @param voltageDipPercentAtSelected the dip this step causes on the selected set; blank when there is none
 * @param withinDipLimit              whether that dip is within {@code dipLimitPercent}; blank when there is no selected set
 * @param dipLimitPercent             the limit this step is held to: the one entered, or for a step with the fire pump
 *                                    the 15% of Article 6.95 less what the cable takes, if that is smaller
 */
public record StartingStepDto(
		int step,
		String motors,
		double startingKva,
		double runningKvaBefore,
		double requiredGeneratorKva,
		Double voltageDipPercentAtSelected,
		Boolean withinDipLimit,
		double dipLimitPercent) {

	public static StartingStepDto from(StartingStep step) {
		return new StartingStepDto(
				step.step(), step.motors(), step.startingKva(), step.runningKvaBefore(),
				step.requiredGeneratorKva(), step.voltageDipPercentAtSelected(), step.withinDipLimit(),
				step.dipLimitPercent());
	}

}
