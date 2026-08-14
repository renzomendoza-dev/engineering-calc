package com.renzoproject.calc.core.mechanical.firepump;

import java.util.List;

/**
 * Shape of {@code reference/firepump/motor-hp-steps.json}. Package-private — internal to
 * {@link JsonFirePumpMotorSizeResolver}'s JSON parsing.
 */
record MotorHpStepsFile(String standard, String confidence, String driverType, String unit, List<Double> steps) {

}
