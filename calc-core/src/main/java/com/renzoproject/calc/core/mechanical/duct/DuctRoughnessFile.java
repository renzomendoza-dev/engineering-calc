package com.renzoproject.calc.core.mechanical.duct;

import java.util.List;

/**
 * Top-level shape of {@code reference/duct/duct-roughness.json}. Package-private -- internal to
 * {@link JsonDuctRoughnessResolver}'s JSON parsing.
 */
record DuctRoughnessFile(String standard, String confidence, String source, String unit, List<DuctRoughnessRow> materials) {

}
