package com.renzoproject.calc.core.electrical.conduitfill;

/**
 * A practical, field-experience note on how easy conductors may be to physically pull through
 * conduit at the recommended trade size.
 *
 * <p>This is deliberately, permanently separate from PEC legal fill compliance and must never
 * be presented as a code-derived pass/fail: a conduit can sit well within the legal
 * 40%/31%/53% fill limit and still be genuinely difficult to pull in practice, especially with
 * twisted-pair or stranded conductor bundles. Keep this advisory visually and structurally
 * distinct from {@link ConduitFillResult}'s compliance fields in any UI or documentation that
 * surfaces it.
 *
 * @param mayBeDifficultToPull {@code true} if actual fill at the recommended size exceeds
 *                             {@link #PRACTICAL_PULL_THRESHOLD_PERCENT}
 * @param note                 human-readable explanation when {@code mayBeDifficultToPull} is
 *                             {@code true}; {@code null} when it's {@code false}
 */
public record PracticalFillAdvisory(boolean mayBeDifficultToPull, String note) {

	/**
	 * Flat fill-percentage threshold above which pulling may become physically difficult, per
	 * field experience — NOT sourced from PEC or any code table, and not a legal limit. Does
	 * not vary by conduit type, bend count, or cable construction in this version: it's a
	 * single flat threshold on actual fill percentage. Reasonably subject to revision as more
	 * real-world pull data is gathered.
	 */
	public static final double PRACTICAL_PULL_THRESHOLD_PERCENT = 25.0;

}
