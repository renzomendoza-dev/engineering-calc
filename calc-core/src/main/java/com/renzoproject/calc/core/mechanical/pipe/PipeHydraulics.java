package com.renzoproject.calc.core.mechanical.pipe;

/**
 * Shape of a {@code reference/pipes/{material}.json} file's {@code "hydraulics"} block —
 * see {@code reference/fluids/roughness-patch-instructions.md}. Package-private — internal to
 * {@link JsonPipeDimensionResolver}'s JSON parsing.
 */
record PipeHydraulics(double absoluteRoughnessMm, String confidence, String source) {

}
