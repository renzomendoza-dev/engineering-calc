package com.renzoproject.calc.core.mechanical.duct;

import java.util.List;

/**
 * Top-level shape of {@code reference/duct/duct-velocity-limits.json}. Package-private --
 * internal to {@link JsonDuctVelocityLimitResolver}'s JSON parsing.
 */
record DuctVelocityLimitFile(String standard, String confidence, String source, String unit, String note, List<DuctVelocityLimitRow> limits) {

}
