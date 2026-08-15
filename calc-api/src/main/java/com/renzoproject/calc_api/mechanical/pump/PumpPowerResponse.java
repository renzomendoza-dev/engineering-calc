package com.renzoproject.calc_api.mechanical.pump;

public record PumpPowerResponse(Double hydraulicPowerKw, Double shaftPowerKw, String recommendedMotorSizeKw) {

}
