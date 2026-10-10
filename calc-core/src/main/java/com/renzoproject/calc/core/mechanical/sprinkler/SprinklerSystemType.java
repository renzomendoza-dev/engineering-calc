package com.renzoproject.calc.core.mechanical.sprinkler;

/**
 * Sprinkler system service type. Selects the Hazen-Williams C factor for steel pipe (wet service
 * is less corroded than dry/preaction service, so it carries a higher C).
 */
public enum SprinklerSystemType {
	WET,
	DRY,
	PREACTION
}
