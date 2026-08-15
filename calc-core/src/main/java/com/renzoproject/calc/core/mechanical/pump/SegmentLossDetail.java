package com.renzoproject.calc.core.mechanical.pump;

import com.renzoproject.calc.core.mechanical.pipe.PipePressureLossResult;

public record SegmentLossDetail(PipeSegmentSpec segment, PipePressureLossResult result) {

}
