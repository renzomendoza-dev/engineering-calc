package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Input for {@link SprinklerHydraulicCalculator}. <b>Units:</b> psi, US gpm, feet, ft2 -- see
 * {@code package-info.java}.
 *
 * <p>{@code densityGpmPerSqFt}/{@code designAreaSqFt}/{@code hoseStreamGpm} are supplied by the
 * caller rather than looked up from {@code hazard}: the NFPA 13 density/area curve gives a range of
 * valid points per hazard, so picking one is a design decision. {@code HazardPresetResolver}
 * publishes the usual starting points for a caller (or a UI) to populate these from, and
 * {@code hazard} is carried through so the result can be read in context.
 *
 * @param systemType              wet, dry or preaction
 * @param hazard                  occupancy hazard the design density came from
 * @param densityGpmPerSqFt       design density; must be positive
 * @param designAreaSqFt          area of sprinkler operation the remote area represents; must be
 *                                positive. Not used to pick sprinklers (the caller lists them) --
 *                                carried for the result and for cross-checking
 * @param hoseStreamGpm           hose stream allowance, gpm; must not be negative. Added at
 *                                {@code hoseStreamNodeId}, NOT at the remote sprinkler, since it
 *                                does not flow through the branch lines
 * @param hoseStreamNodeId        node the hose allowance is added at; required when
 *                                {@code hoseStreamGpm > 0}
 * @param assumedStartPressurePsi starting pressure assumed at the most remote sprinkler. The
 *                                sprinkler's actual starting pressure is the greater of this and
 *                                the pressure its minimum required flow needs; pass {@code 0} to
 *                                let the minimum flow govern outright
 * @param sprinklers              flowing sprinklers in the remote area; at least one, node ids
 *                                unique
 * @param segments                pipe network; at least one, {@code (from, to)} pairs unique
 * @param waterSupply             where the water comes from -- a flow test ({@link SupplyCurve})
 *                                or an elevated tank ({@link GravityTankSupply}). <b>May be
 *                                null</b>, for a system whose supply isn't known yet or has no
 *                                pressure of its own (a cistern awaiting a pump): the demand is
 *                                still calculated, and the margin, verdict and supply curve come
 *                                back null rather than guessed
 * @param deviceLosses            fixed device losses added before comparing to the supply; may be
 *                                empty or null
 * @param identicalBranches       branches declared as copies of another instead of listed segment
 *                                by segment (see {@link IdenticalBranch}); may be empty or null
 * @param listedMinimumPressurePsi the minimum operating pressure the sprinkler's listing specifies
 *                                (NFPA 13 27.2.4.11.2), applied to every sprinkler; {@code 0} for
 *                                none. It can only raise the floor above the standard's own
 * @throws CalculationException if any rule above is violated
 */
public record SprinklerHydraulicInput(
		SprinklerSystemType systemType,
		SprinklerHazard hazard,
		double densityGpmPerSqFt,
		double designAreaSqFt,
		double hoseStreamGpm,
		String hoseStreamNodeId,
		double assumedStartPressurePsi,
		List<RemoteAreaSprinkler> sprinklers,
		List<SprinklerPipeSegment> segments,
		WaterSupply waterSupply,
		List<DeviceLoss> deviceLosses,
		List<IdenticalBranch> identicalBranches,
		double listedMinimumPressurePsi) {

	/** A network with identical branches declared and no listing-specific minimum pressure. */
	public SprinklerHydraulicInput(
			SprinklerSystemType systemType,
			SprinklerHazard hazard,
			double densityGpmPerSqFt,
			double designAreaSqFt,
			double hoseStreamGpm,
			String hoseStreamNodeId,
			double assumedStartPressurePsi,
			List<RemoteAreaSprinkler> sprinklers,
			List<SprinklerPipeSegment> segments,
			WaterSupply waterSupply,
			List<DeviceLoss> deviceLosses,
			List<IdenticalBranch> identicalBranches) {
		this(systemType, hazard, densityGpmPerSqFt, designAreaSqFt, hoseStreamGpm, hoseStreamNodeId,
				assumedStartPressurePsi, sprinklers, segments, waterSupply, deviceLosses, identicalBranches, 0.0);
	}

	/** A network with every sprinkler and pipe listed -- no identical branches. */
	public SprinklerHydraulicInput(
			SprinklerSystemType systemType,
			SprinklerHazard hazard,
			double densityGpmPerSqFt,
			double designAreaSqFt,
			double hoseStreamGpm,
			String hoseStreamNodeId,
			double assumedStartPressurePsi,
			List<RemoteAreaSprinkler> sprinklers,
			List<SprinklerPipeSegment> segments,
			WaterSupply waterSupply,
			List<DeviceLoss> deviceLosses) {
		this(systemType, hazard, densityGpmPerSqFt, designAreaSqFt, hoseStreamGpm, hoseStreamNodeId,
				assumedStartPressurePsi, sprinklers, segments, waterSupply, deviceLosses, List.of(), 0.0);
	}

	public SprinklerHydraulicInput {
		if (systemType == null) {
			throw new CalculationException("systemType is required");
		}
		if (hazard == null) {
			throw new CalculationException("hazard is required");
		}
		if (densityGpmPerSqFt <= 0) {
			throw new CalculationException("densityGpmPerSqFt must be positive");
		}
		if (designAreaSqFt <= 0) {
			throw new CalculationException("designAreaSqFt must be positive");
		}
		if (hoseStreamGpm < 0) {
			throw new CalculationException("hoseStreamGpm must not be negative");
		}
		if (hoseStreamGpm > 0 && (hoseStreamNodeId == null || hoseStreamNodeId.isBlank())) {
			throw new CalculationException("hoseStreamNodeId is required when hoseStreamGpm is greater than zero");
		}
		if (assumedStartPressurePsi < 0) {
			throw new CalculationException("assumedStartPressurePsi must not be negative");
		}
		if (listedMinimumPressurePsi < 0) {
			throw new CalculationException("listedMinimumPressurePsi must not be negative");
		}
		if (sprinklers == null || sprinklers.isEmpty()) {
			throw new CalculationException("at least one sprinkler is required");
		}
		if (segments == null || segments.isEmpty()) {
			throw new CalculationException("at least one pipe segment is required");
		}
		// waterSupply is deliberately optional -- see the record's Javadoc.

		Set<String> sprinklerNodes = new HashSet<>();
		for (RemoteAreaSprinkler sprinkler : sprinklers) {
			if (!sprinklerNodes.add(sprinkler.nodeId())) {
				throw new CalculationException("more than one sprinkler listed at node " + sprinkler.nodeId()
						+ " -- combine them into one sprinkler or give them separate nodes");
			}
		}

		Set<String> segmentKeys = new HashSet<>();
		for (SprinklerPipeSegment segment : segments) {
			if (!segmentKeys.add(segment.fromNodeId() + "->" + segment.toNodeId())) {
				throw new CalculationException("duplicate pipe segment " + segment.fromNodeId() + "->"
						+ segment.toNodeId());
			}
		}

		sprinklers = List.copyOf(sprinklers);
		segments = List.copyOf(segments);
		deviceLosses = deviceLosses == null ? List.of() : List.copyOf(deviceLosses);
		identicalBranches = identicalBranches == null ? List.of() : List.copyOf(identicalBranches);
	}

}
