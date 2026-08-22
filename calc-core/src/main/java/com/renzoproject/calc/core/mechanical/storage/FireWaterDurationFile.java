package com.renzoproject.calc.core.mechanical.storage;

import java.util.List;

/**
 * Top-level shape of {@code reference/storage/fire-water-duration.json}. Package-private --
 * internal to {@link JsonFireWaterDurationResolver}'s JSON parsing.
 */
record FireWaterDurationFile(String standard, String confidence, String note, List<ClassificationRow> classifications) {

}
