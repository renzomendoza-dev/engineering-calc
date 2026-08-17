package com.renzoproject.calc.core.smokecontrol;

/**
 * Deliberate duplicate of {@link PlumeRegime} for {@link TSquaredSmokeProductionCalculator}, not
 * a shared/refactored type -- this calculator is intentionally fully separate from
 * {@link SmokeProductionCalculator} (different input shape, different growth model), so it
 * doesn't reuse that calculator's sealed hierarchy even though the underlying NFPA 92 plume
 * correlation is identical. See {@link TSquaredFarField}/{@link TSquaredNearField} for the
 * formulas -- same as {@link FarField}/{@link NearField}, just not the same classes.
 */
public sealed interface TSquaredPlumeRegime permits TSquaredFarField, TSquaredNearField {

	double massFlowRateKgS();

}
