package com.renzoproject.calc.core.mechanical.sprinkler;

/**
 * One row of the hydraulic calculation sheet: what flows through a segment and what it costs.
 * Mirrors the columns of a hand-worked NFPA 13 calculation sheet so a reviewer can follow it
 * line by line. <b>Units:</b> psi, gpm, ft, in, fps.
 *
 * @param fromNodeId                 upstream node
 * @param toNodeId                   downstream node
 * @param flowGpm                    flow carried by this segment, after any junction balancing
 * @param nominalSize                nominal size as supplied
 * @param insideDiameterInch         actual inside diameter resolved from reference data
 * @param cFactor                    Hazen-Williams C used
 * @param pipeLengthFt               centerline pipe length
 * @param fittingEquivalentLengthFt  total equivalent length of this segment's fittings, after the
 *                                   C factor adjustment below
 * @param fittingCFactorAdjustment   multiplier applied to lengths looked up from the published
 *                                   chart, which assumes C=120 ({@code (C/120)^1.85}). {@code 1.0}
 *                                   when the segment is C=120 or when every fitting carried an
 *                                   explicit length, which is never adjusted. Reported so a
 *                                   reviewer can reconcile the row against the printed table
 * @param totalLengthFt              {@code pipeLengthFt + fittingEquivalentLengthFt}
 * @param frictionLossPsiPerFoot     Hazen-Williams unit loss at {@code flowGpm}
 * @param frictionLossPsi            {@code frictionLossPsiPerFoot * totalLengthFt}
 * @param elevationPressurePsi       static pressure for this segment's rise (negative for a drop)
 * @param endPressurePsi             pressure at {@code toNodeId} after friction and elevation
 * @param velocityFps                flow velocity, for the 20 fps check
 */
public record SegmentFlowRow(
		String fromNodeId,
		String toNodeId,
		double flowGpm,
		String nominalSize,
		double insideDiameterInch,
		double cFactor,
		double pipeLengthFt,
		double fittingEquivalentLengthFt,
		double fittingCFactorAdjustment,
		double totalLengthFt,
		double frictionLossPsiPerFoot,
		double frictionLossPsi,
		double elevationPressurePsi,
		double endPressurePsi,
		double velocityFps) {

}
