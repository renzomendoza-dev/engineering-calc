package com.renzoproject.calc_api.mechanical.reference;

import com.renzoproject.calc.core.mechanical.pipe.PipeScheduleReference;

import java.util.List;

/** HTTP response representation of a {@code PipeScheduleReference}. */
public record PipeScheduleDto(String schedule, List<PipeSizeDto> sizes) {

	public static PipeScheduleDto from(PipeScheduleReference group) {
		return new PipeScheduleDto(group.schedule(), group.sizes().stream().map(PipeSizeDto::from).toList());
	}

}
