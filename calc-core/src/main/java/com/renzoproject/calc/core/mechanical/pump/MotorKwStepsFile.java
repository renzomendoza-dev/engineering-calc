package com.renzoproject.calc.core.mechanical.pump;

import java.util.List;

/**
 * Shape of {@code reference/pump/motor-kw-steps.json}. Package-private — internal to
 * {@link JsonPumpMotorSizeResolver}'s JSON parsing.
 */
record MotorKwStepsFile(String standard, String confidence, String driverType, String unit, List<Double> steps) {

}
