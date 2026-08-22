package com.renzoproject.calc.core.mechanical.tank;

/** Which pump control scheme {@link PressureTankCalculator} is sizing a tank for. */
public enum PumpControlType {
	/** On/off pressure switch -- the pump runs at full flow between cut-in and cut-out. */
	CONVENTIONAL,
	/** Variable frequency drive -- the pump continuously modulates and sleeps at low demand. */
	VFD
}
