package com.renzoproject.calc.core.mechanical.standpipe;

import com.renzoproject.calc.core.exception.CalculationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The piping of one fire department connection (FDC), from the system to the connection. Class I
 * and III systems are to be designed so that the system demand can be supplied by each FDC
 * (10.4.1), and hydraulic calculations should show that each connection, or several together where
 * 10.7.3.2 and 10.7.3.3 allow, can supply it (A.10.4.1).
 *
 * <p>Segments are listed from the end that joins the system to the connection, so the first leaves
 * the supply end of the main path (the base of the riser). The pressure the fire department must
 * deliver at the connection is the pressure at the supply end plus the friction and elevation of
 * these pipes, at the flow they carry. A segment's {@code elevationRiseFt} is how many feet higher
 * its system-side end is than its connection-side end, as everywhere else in this calculation.
 *
 * @param label    name shown in results, e.g. {@code "FDC on the street side"}; not blank
 * @param flowGpm  flow this connection carries, gpm; nullable, in which case it carries the whole flow
 *                 at the supply (each connection able to supply the demand, 10.4.1). Give a smaller
 *                 flow where several connections share the demand (10.7.3.2)
 * @param inlets   inlets of the chosen size on this connection; nullable. When given, they are checked
 *                 against the flow the connection carries (10.7.3)
 * @param segments the pipes, system end first; at least one, none with a hose connection or added flow
 */
public record StandpipeFdcLine(
		String label,
		Double flowGpm,
		Integer inlets,
		List<StandpipeSegment> segments) {

	public StandpipeFdcLine {
		if (label == null || label.isBlank()) {
			throw new CalculationException("fire department connection label must not be blank");
		}
		if (flowGpm != null && flowGpm <= 0) {
			throw new CalculationException("fire department connection " + label + ": flowGpm must be positive");
		}
		if (inlets != null && inlets < 0) {
			throw new CalculationException("fire department connection " + label + ": inlets must not be negative");
		}
		if (segments == null || segments.isEmpty()) {
			throw new CalculationException("fire department connection " + label + ": at least one pipe segment is required");
		}
		segments = List.copyOf(segments);
		Set<String> labels = new HashSet<>();
		for (StandpipeSegment segment : segments) {
			if (segment.hoseConnectionSize() != null || segment.flowAddedGpm() > 0 || segment.otherStandpipeConnection()) {
				throw new CalculationException("fire department connection " + label + ": segment " + segment.label()
						+ " must be plain pipe, with no hose connection or added flow");
			}
			if (!labels.add(segment.label())) {
				throw new CalculationException("fire department connection " + label + ": segment label used twice: " + segment.label());
			}
		}
	}

}
