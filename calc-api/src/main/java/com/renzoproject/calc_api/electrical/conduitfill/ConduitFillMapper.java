package com.renzoproject.calc_api.electrical.conduitfill;

import com.renzoproject.calc.core.electrical.conduitfill.ConductorFillEntry;
import com.renzoproject.calc.core.electrical.conduitfill.ConduitFillInput;
import com.renzoproject.calc.core.electrical.conduitfill.ConduitFillResult;
import com.renzoproject.calc.core.electrical.reference.ConduitType;
import com.renzoproject.calc.core.electrical.reference.InsulationType;

import java.util.List;

/**
 * Maps between calc-api's conduit fill DTOs and calc-core's calculator types.
 *
 * <p>{@code InsulationType.fromLabel} / {@code ConduitType.fromLabel} already throw
 * {@code CalculationException} with a clear "Unknown ... type: X" message on an unmatched
 * label, which {@code GlobalExceptionHandler} turns into a 400 — no extra mapping logic needed
 * here beyond calling them.
 */
public final class ConduitFillMapper {

	private ConduitFillMapper() {
	}

	public static ConduitFillInput toInput(ConduitFillRequest request) {
		List<ConductorFillEntry> conductors = request.conductors().stream()
				.map(ConduitFillMapper::toEntry)
				.toList();
		ConduitType conduitType = ConduitType.fromLabel(request.conduitType());
		return new ConduitFillInput(conductors, conduitType);
	}

	private static ConductorFillEntry toEntry(ConductorFillEntryDto dto) {
		InsulationType insulationType = InsulationType.fromLabel(dto.insulationType());
		return new ConductorFillEntry(insulationType, dto.sizeLabel(), dto.quantity());
	}

	public static ConduitFillResponse toResponse(ConduitFillResult result) {
		return ConduitFillResponse.from(result);
	}

}
