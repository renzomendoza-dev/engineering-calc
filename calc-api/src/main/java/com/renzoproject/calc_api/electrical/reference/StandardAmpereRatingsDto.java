package com.renzoproject.calc_api.electrical.reference;

import com.renzoproject.calc.core.electrical.reference.StandardAmpereRatingTable;
import java.util.List;

/**
 * PEC 2017 Table 2.40.1.6(A), the standard ampere ratings of fuses and inverse time circuit
 * breakers, for the frontend reference table: the table's ratings ascending, the ratings
 * 2.40.1.6(A) adds for fuses only, and the 800 A ceiling 2.40.1.4(B) puts on taking the next
 * rating up. Display only.
 */
public record StandardAmpereRatingsDto(
		List<Integer> ratings,
		List<Integer> additionalFuseRatings,
		int nextHigherRatingMaximumAmps) {

	public static StandardAmpereRatingsDto from(StandardAmpereRatingTable table) {
		return new StandardAmpereRatingsDto(table.ratings(), table.additionalFuseRatings(), table.nextHigherRatingMaximumAmps());
	}

}
