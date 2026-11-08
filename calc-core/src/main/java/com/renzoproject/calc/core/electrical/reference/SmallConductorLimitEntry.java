package com.renzoproject.calc.core.electrical.reference;

/**
 * One line of PEC 2.40.1.4(D): the most the overcurrent protection may be for a small conductor.
 *
 * @param conductorMaterial            {@code ALUMINUM} also stands for copper-clad aluminum
 * @param sizeLabel                    size as published in the PEC tables, e.g. {@code "2.0"}
 * @param maximumOvercurrentDeviceAmps largest permitted rating of the overcurrent device, A
 * @param conditions                   conditions the printed line attaches to the limit; null for none
 */
public record SmallConductorLimitEntry(
		ConductorMaterial conductorMaterial,
		String sizeLabel,
		int maximumOvercurrentDeviceAmps,
		String conditions) {

}
