package com.renzoproject.calc.core.mechanical.sprinkler;

import java.util.List;

/**
 * Publishes nominal sprinkler K-factors for a form's dropdown.
 *
 * <p>Deliberately list-only: {@link SprinklerHydraulicCalculator} always takes K from the caller's
 * {@link RemoteAreaSprinkler}, because a listed sprinkler's actual K comes from its data sheet and
 * may differ from the nominal value.
 */
public interface SprinklerKFactorResolver {

	List<SprinklerKFactorRow> allEntries();

}
