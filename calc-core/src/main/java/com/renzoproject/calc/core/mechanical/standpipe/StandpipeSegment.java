package com.renzoproject.calc.core.mechanical.standpipe;

import com.renzoproject.calc.core.exception.CalculationException;
import java.util.List;

/**
 * One run of pipe on the path from the hydraulically most remote hose connection back to the
 * supply. Segments are listed in that order, like the rows of a hydraulic calculation sheet: the
 * first is the piping at the remote connection, the last ends at the supply (the base of the riser
 * or the pump discharge).
 *
 * @param label              name shown in results, e.g. {@code "Hose valve to riser"}; not blank
 * @param role               what the pipe is, for the minimum-size rules
 * @param flowAddedGpm       flow entering at the remote end of this segment, gpm; nullable (none)
 * @param hoseConnectionSize when not null, {@code flowAddedGpm} is the discharge of a hose connection
 *                           of this size at the remote end of the segment, which must then also be
 *                           given. Its residual pressure is the minimum for that size (10.2.6.1)
 * @param otherStandpipeConnection when true, {@code flowAddedGpm} is the flow of another standpipe of the building, added at
 *                           its connection point to the common supply piping at the remote end of this segment (10.6.1.2.1).
 *                           The pressure there is held at the minimum residual, and the flow is not balanced to a higher
 *                           pressure (10.6.1.2.4). Needs a flow, and is not a hose connection
 * @param pipeMaterial       material code for {@code PipeDimensionResolver} (e.g. {@code BI})
 * @param schedule           schedule code (e.g. {@code SCH40})
 * @param nominalSize        nominal size as published in {@code reference/pipes/*.json}; the inside
 *                           diameter is resolved from reference data, never taken from the nominal size
 * @param cFactor            Hazen-Williams C; must be positive
 * @param pipeLengthFt       centreline length, ft; not negative
 * @param elevationRiseFt    how many feet higher the remote end of the segment is than its supply end
 *                           (the rise of the water on its way to the hose connection), ft. Positive
 *                           for a riser, which costs pressure; negative where the pipe falls
 * @param fittings           fittings on the segment; null or empty for none
 */
public record StandpipeSegment(
		String label,
		PipeRole role,
		Double flowAddedGpm,
		HoseConnectionSize hoseConnectionSize,
		boolean otherStandpipeConnection,
		String pipeMaterial,
		String schedule,
		String nominalSize,
		double cFactor,
		double pipeLengthFt,
		double elevationRiseFt,
		List<StandpipeFitting> fittings) {

	public StandpipeSegment {
		if (label == null || label.isBlank()) {
			throw new CalculationException("segment label must not be blank");
		}
		if (role == null) {
			throw new CalculationException("segment " + label + ": role is required");
		}
		if (flowAddedGpm == null) {
			flowAddedGpm = 0.0;
		}
		if (flowAddedGpm < 0) {
			throw new CalculationException("segment " + label + ": flowAddedGpm must not be negative");
		}
		if (hoseConnectionSize != null && flowAddedGpm <= 0) {
			throw new CalculationException("segment " + label + ": a hose connection needs a flow");
		}
		if (otherStandpipeConnection && hoseConnectionSize != null) {
			throw new CalculationException("segment " + label + ": the flow added is either a hose connection's or another standpipe's, not both");
		}
		if (otherStandpipeConnection && flowAddedGpm <= 0) {
			throw new CalculationException("segment " + label + ": another standpipe's connection point needs its flow");
		}
		if (pipeMaterial == null || pipeMaterial.isBlank()) {
			throw new CalculationException("segment " + label + ": pipeMaterial is required");
		}
		if (nominalSize == null || nominalSize.isBlank()) {
			throw new CalculationException("segment " + label + ": nominalSize is required");
		}
		if (cFactor <= 0) {
			throw new CalculationException("segment " + label + ": cFactor must be positive");
		}
		if (pipeLengthFt < 0) {
			throw new CalculationException("segment " + label + ": pipeLengthFt must not be negative");
		}
		fittings = fittings == null ? List.of() : List.copyOf(fittings);
	}

}
