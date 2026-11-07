package com.renzoproject.calc.core.mechanical.standpipe;

import com.renzoproject.calc.core.exception.CalculationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Input for {@link StandpipeHydraulicCalculator}: the demand rules of NFPA 14 for a class of
 * system, and the pipe path from the most remote hose connection back to the supply.
 *
 * <p>Units: psi, US gpm, feet, inches. Rule values come from the printed pages of the 2024 edition (Chapters 5, 8, 10 and 11).
 *
 * @param standpipeClass                       class of system
 * @param sprinklerProtection                  how much of the building is sprinklered
 * @param sprinklerHazard                      light or ordinary; required when partly sprinklered and a sprinkler demand is given
 * @param sprinklerDemandGpm                   hydraulically calculated sprinkler demand, including its hose stream, gpm; nullable
 * @param additionalStandpipes                 standpipes in the building besides the hydraulically most remote one
 * @param floorAreaOver80000SqFt               whether the floors exceed 80,000 sq ft each (10.6.1.1.4.1)
 * @param horizontalStandpipeOnThreeConnections whether a horizontal standpipe serves three or more connections on a floor (10.6.1.1.3)
 * @param singleConnectionLaterals             lateral pipes that each serve a single hose connection; each is treated as served from
 *                                             a separate standpipe for the minimum flow (10.6.1.1.7). Class I and III only
 * @param risersEndAtDifferentLevels           whether the standpipes end at different floor levels, which needs a separate
 *                                             calculation for the standpipes on each level (10.6.1.2.1.1)
 * @param manualSystem                         whether the system is manual, so the path ends at the fire department connection and
 *                                             the pressure found is the pressure the fire department must supply there
 *                                             (10.2.6.1.2). Class I and III only
 * @param fireDepartmentConnectionInletSize    size of the inlets provided; nullable, defaults to 2.5 in.
 * @param fireDepartmentConnectionInlets       inlets of that size provided on the fire department connections; nullable
 * @param supplyDurationMinutes                duration the supply is sized for, minutes; nullable, defaults to 30 (5.2)
 * @param flowAddedAtSupplyGpm                 flow that joins at the supply end of the path with no friction of its own
 *                                             (other standpipes, the sprinkler allowance), gpm; nullable. When omitted for
 *                                             Class I and III it is worked out as the required flow less the flow on the
 *                                             path; for Class II, which has no combining rule on the pages read, it is zero.
 *                                             A figure given is used as it is
 * @param segments                             the path, remote connection first; at least one
 */
public record StandpipeHydraulicInput(
		StandpipeClass standpipeClass,
		SprinklerProtection sprinklerProtection,
		CombinedHazard sprinklerHazard,
		Double sprinklerDemandGpm,
		int additionalStandpipes,
		boolean floorAreaOver80000SqFt,
		boolean horizontalStandpipeOnThreeConnections,
		int singleConnectionLaterals,
		boolean risersEndAtDifferentLevels,
		boolean manualSystem,
		InletSize fireDepartmentConnectionInletSize,
		Integer fireDepartmentConnectionInlets,
		Double supplyDurationMinutes,
		Double flowAddedAtSupplyGpm,
		List<StandpipeSegment> segments) {

	public static final double DEFAULT_SUPPLY_DURATION_MINUTES = 30.0;

	public StandpipeHydraulicInput {
		if (standpipeClass == null) {
			throw new CalculationException("standpipeClass is required");
		}
		if (sprinklerProtection == null) {
			sprinklerProtection = SprinklerProtection.NONE;
		}
		if (sprinklerDemandGpm != null && sprinklerDemandGpm < 0) {
			throw new CalculationException("sprinklerDemandGpm must not be negative");
		}
		if (sprinklerProtection == SprinklerProtection.PARTIAL && sprinklerDemandGpm != null && sprinklerHazard == null) {
			throw new CalculationException("sprinklerHazard (light or ordinary) is required for a partly sprinklered building");
		}
		if (additionalStandpipes < 0) {
			throw new CalculationException("additionalStandpipes must not be negative");
		}
		if (singleConnectionLaterals < 0) {
			throw new CalculationException("singleConnectionLaterals must not be negative");
		}
		if (manualSystem && standpipeClass == StandpipeClass.CLASS_II) {
			throw new CalculationException("a manual system is for Class I or Class III; a Class II system is automatic");
		}
		if (fireDepartmentConnectionInletSize == null) {
			fireDepartmentConnectionInletSize = InletSize.TWO_AND_HALF_INCH;
		}
		if (fireDepartmentConnectionInlets != null && fireDepartmentConnectionInlets < 0) {
			throw new CalculationException("fireDepartmentConnectionInlets must not be negative");
		}
		if (supplyDurationMinutes == null) {
			supplyDurationMinutes = DEFAULT_SUPPLY_DURATION_MINUTES;
		}
		if (supplyDurationMinutes <= 0) {
			throw new CalculationException("supplyDurationMinutes must be positive");
		}
		if (flowAddedAtSupplyGpm != null && flowAddedAtSupplyGpm < 0) {
			throw new CalculationException("flowAddedAtSupplyGpm must not be negative");
		}
		if (segments == null || segments.isEmpty()) {
			throw new CalculationException("at least one pipe segment is required");
		}
		segments = List.copyOf(segments);
		Set<String> labels = new HashSet<>();
		for (StandpipeSegment segment : segments) {
			if (!labels.add(segment.label())) {
				throw new CalculationException("segment label used twice: " + segment.label());
			}
		}
		if (segments.stream().noneMatch(segment -> segment.hoseConnectionSize() != null)) {
			throw new CalculationException("at least one segment must carry a hose connection");
		}
	}

}
