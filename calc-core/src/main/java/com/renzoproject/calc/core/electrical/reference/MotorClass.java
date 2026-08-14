package com.renzoproject.calc.core.electrical.reference;

/**
 * Three-phase motor construction class. Only distinguished for {@link MotorPhaseType#THREE_PHASE}
 * — synchronous motor FLC data only exists at 25 HP and above, per PEC Table 4.30.14.4.
 */
public enum MotorClass {
	INDUCTION,
	SYNCHRONOUS
}
