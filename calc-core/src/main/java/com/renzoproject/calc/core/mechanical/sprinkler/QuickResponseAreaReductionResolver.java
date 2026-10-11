package com.renzoproject.calc.core.mechanical.sprinkler;

/**
 * Supplies NFPA 13's quick-response design area reduction curve. Kept behind an interface like the
 * other sprinkler reference data so the calculator can be tested against a fake curve.
 */
public interface QuickResponseAreaReductionResolver {

	QuickResponseAreaReductionCurve curve();

}
