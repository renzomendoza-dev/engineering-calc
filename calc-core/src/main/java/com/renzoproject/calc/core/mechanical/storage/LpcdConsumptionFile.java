package com.renzoproject.calc.core.mechanical.storage;

import java.util.List;

/**
 * Top-level shape of {@code reference/storage/lpcd-consumption.json}. Package-private -- internal
 * to {@link JsonPerCapitaConsumptionResolver}'s JSON parsing.
 */
record LpcdConsumptionFile(String source, String confidence, List<String> verifyAgainst, String unit, List<OccupancyTypeRow> occupancyTypes) {

}
