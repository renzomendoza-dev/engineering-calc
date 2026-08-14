package com.renzoproject.calc_api.mechanical.firepump;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FirePumpCapacityRequest(@NotNull @Positive Double ratedFlowGpm) {

}
