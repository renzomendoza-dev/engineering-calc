package com.renzoproject.calc.core.mechanical.firepump;

import java.util.List;

/**
 * Shape of {@code reference/firepump/standard-capacities.json}. Package-private — internal to
 * {@link JsonFirePumpCapacityResolver}'s JSON parsing.
 */
record StandardCapacitiesFile(String standard, String confidence, String unit, List<Integer> capacities) {

}
