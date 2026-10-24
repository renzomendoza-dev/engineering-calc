package com.renzoproject.calc.core.electrical.generatorset;

/**
 * What a row of the generator set load schedule is, so a caller can group or colour them.
 * {@code BUILDING} and {@code FIRE_PUMP} are the two loads that can be entered as a total
 * ({@link GeneratorSetSizingInput.DirectLoad}).
 */
public enum LoadKind {
	LIGHTING,
	RECEPTACLE,
	OTHER,
	MOTOR,
	BUILDING,
	FIRE_PUMP
}
