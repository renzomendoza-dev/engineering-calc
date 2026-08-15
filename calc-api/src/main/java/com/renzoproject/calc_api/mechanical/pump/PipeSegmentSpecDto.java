package com.renzoproject.calc_api.mechanical.pump;

import com.renzoproject.calc_api.mechanical.pipe.DiameterSpecTypeDto;
import com.renzoproject.calc_api.mechanical.pipe.FrictionFactorMethodDto;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * One pipe segment in a {@link PumpTDHRequest}'s suction or discharge line.
 * {@code diameterSpecType}/{@code nominalMaterial}/{@code nominalSchedule}/{@code nominalLabel}/
 * {@code rawDiameterValue}/{@code rawDiameterUnit} are the exact same field names/shape
 * {@code PipePressureLossRequest} already uses for its {@code DiameterSpec} discriminator —
 * {@link DiameterSpecTypeDto} is reused directly, not redeclared. {@link FrictionFactorMethodDto}
 * is likewise reused from the pressure-loss endpoint.
 */
public record PipeSegmentSpecDto(
		@NotNull DiameterSpecTypeDto diameterSpecType,
		String nominalMaterial,
		String nominalSchedule,
		String nominalLabel,
		Double rawDiameterValue,
		String rawDiameterUnit,
		@NotNull @Positive Double lengthMeters,
		@NotNull FrictionFactorMethodDto method) {

	@AssertTrue(message = "For NOMINAL diameterSpecType: nominalMaterial and nominalLabel are "
			+ "required. For RAW: rawDiameterValue and rawDiameterUnit are required.")
	public boolean isDiameterSpecFieldsValid() {
		if (diameterSpecType == null) {
			return true; // let @NotNull on diameterSpecType itself report this
		}
		return switch (diameterSpecType) {
			case NOMINAL -> notBlank(nominalMaterial) && notBlank(nominalLabel);
			case RAW -> rawDiameterValue != null && notBlank(rawDiameterUnit);
		};
	}

	private static boolean notBlank(String value) {
		return value != null && !value.isBlank();
	}

}
