package com.renzoproject.calc.core.electrical.reference;

import java.util.List;

/**
 * Shape of {@code reference/electrical/small-conductor-limits.json}. Package-private: internal to
 * {@link SmallConductorLimitTable}.
 */
record SmallConductorLimitFile(
		String standard,
		String edition,
		String confidence,
		String note,
		List<SmallConductorLimitEntry> limits) {

}
