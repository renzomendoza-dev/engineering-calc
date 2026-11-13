package com.renzoproject.calc.core.electrical.reference;

import com.renzoproject.calc.core.reference.ReferenceJson;
import java.util.List;

/**
 * PEC 2017 Table 2.40.1.6(A), the standard ampere ratings of fuses and inverse time circuit
 * breakers, and the rule of 2.40.1.4(B) and (C) for the largest of them that may protect a
 * conductor of a given ampacity.
 */
public class StandardAmpereRatingTable {

	private static final String RESOURCE_PATH = "/reference/electrical/standard-ampere-ratings.json";

	/** An ampacity within this of a rating is taken to correspond with it. */
	private static final double MATCH_TOLERANCE = 1e-9;

	private final StandardAmpereRatingFile file = ReferenceJson.read(RESOURCE_PATH, StandardAmpereRatingFile.class);

	/**
	 * The largest standard rating that may protect conductors of this ampacity.
	 *
	 * <p>A rating equal to the ampacity, or failing that the next higher standard rating where
	 * 2.40.1.4(B) permits it ({@code nextHigherPermitted}, and not above 800 A), or else the largest
	 * standard rating not above the ampacity (2.40.1.4(C) above 800 A, and wherever (B) does not apply).
	 *
	 * @param ampacityAmps        the ampacity of the conductors being protected, after correction and
	 *                            adjustment, all parallel conductors together
	 * @param nextHigherPermitted false for a branch circuit supplying more than one receptacle for
	 *                            cord-and-plug-connected portable loads, 2.40.1.4(B)(1)
	 * @return the rating in amperes, or {@code null} when no standard rating fits (an ampacity below
	 *         15 A where the next higher rating is not permitted)
	 */
	public Integer largestPermittedRating(double ampacityAmps, boolean nextHigherPermitted) {
		Integer largestNotAbove = null;
		for (int rating : file.ratings()) {
			if (Math.abs(rating - ampacityAmps) <= MATCH_TOLERANCE) {
				return rating;
			}
			if (rating < ampacityAmps) {
				largestNotAbove = rating;
				continue;
			}
			// The first rating above the ampacity: the next higher standard rating.
			if (nextHigherPermitted && rating <= file.nextHigherRatingMaximumAmps()) {
				return rating;
			}
			return largestNotAbove;
		}
		return largestNotAbove;
	}

	/** The ratings of Table 2.40.1.6(A), ascending. */
	public List<Integer> ratings() {
		return file.ratings();
	}

	/** The ratings 2.40.1.6(A) adds for fuses only. */
	public List<Integer> additionalFuseRatings() {
		return file.additionalFuseRatings();
	}

	/** The largest rating 2.40.1.4(B) lets be the next one up: 800 A. */
	public int nextHigherRatingMaximumAmps() {
		return file.nextHigherRatingMaximumAmps();
	}

	/** Where the table was read from and how far to trust it, for display next to it. */
	public String standard() {
		return file.standard();
	}

	public String note() {
		return file.note();
	}

}
