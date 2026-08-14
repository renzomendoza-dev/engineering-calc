package com.renzoproject.calc_api.mechanical.reference;

import com.renzoproject.calc.core.mechanical.pipe.PipeMaterialReference;

import java.util.List;

/**
 * HTTP response representation of a {@code PipeMaterialReference}, for populating the pipe
 * velocity/sizing calculator's material -> schedule -> nominal size dropdown chain.
 *
 * <p>{@code confidence} is {@code "verified"} or {@code "placeholder"} — the frontend should
 * show a caveat near the dropdown for {@code "placeholder"} materials (currently uPVC, PPR)
 * rather than hiding it.
 */
public record PipeMaterialDto(String material, String materialName, String confidence, List<PipeScheduleDto> schedules) {

	public static PipeMaterialDto from(PipeMaterialReference reference) {
		return new PipeMaterialDto(
				reference.material(),
				reference.materialName(),
				reference.confidence(),
				reference.schedules().stream().map(PipeScheduleDto::from).toList());
	}

}
