package com.renzoproject.calc.core.mechanical.storage;

import java.util.List;

/**
 * Top-level shape of {@code reference/storage/wsfu-demand.json}. Package-private -- internal to
 * {@link JsonFixtureUnitDemandResolver}'s JSON parsing.
 */
record WsfuDemandFile(String standard, String source, String confidence, String unit, List<WsfuDemandRow> demandTable) {

}
