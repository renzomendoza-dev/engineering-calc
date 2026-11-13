package com.renzoproject.calc.core.mechanical.standpipe;

import com.renzoproject.calc.core.compliance.ComplianceCheck;
import java.util.List;

/**
 * Result of {@link StandpipeHydraulicCalculator}. Units: psi, US gpm, feet, inches, ft/s, US gallons.
 *
 * @param demandLines           how the required flow was built up, in order
 * @param requiredFlowGpm       the flow the system must be able to deliver (10.6)
 * @param requiredFlowCapped    whether the standpipe flow was limited to the maximum for the building (10.6.1.1.6)
 * @param mostRemoteStandpipeFlowGpm the flow required of the most remote standpipe alone
 * @param flowAtSupplyGpm       flow the path delivers at the supply: the segment flows plus the flow added at the supply
 * @param flowAddedAtSupplyGpm  the part of that flow that joins at the supply with no friction of its own
 * @param flowAddedAtSupplyAutomatic whether it was worked out from the demand rules, not given
 * @param pressureAtSupplyPsi   pressure required at the supply end of the path, at that flow
 * @param totalFrictionLossPsi  friction over the whole path
 * @param totalElevationPsi     elevation pressure over the whole path
 * @param requiredFdcInlets     fire department connection inlets of {@code fdcInletSize}, one per 250 gpm for 2.5 in. (10.7.3),
 *                              500 gpm for 4 in. and 750 gpm for 5 in.
 * @param fdcInletSize          the inlet size the count is for
 * @param manualSystem          whether the system is manual, so the pressure at the supply is the pressure needed at the fire
 *                              department connection (10.2.6.1.2)
 * @param supplyDurationMinutes duration used for the volume
 * @param waterSupplyVolumeGal  {@code flowAtSupply x duration}
 * @param fdcLines              one entry per fire department connection given, in input order; empty when none
 * @param highestFdcPressurePsi the highest pressure required at any of those connections; null when none
 * @param staticPressureAtLowestConnectionPsi the static pressure at the lowest hose connection: the static pressure at the supply
 *                              less 0.433 psi per foot of its height above the supply; null when no static pressure was given
 * @param segments              one row per input segment, in input order
 * @param checks                compliance lines
 */
public record StandpipeHydraulicResult(
		List<DemandLine> demandLines,
		double requiredFlowGpm,
		boolean requiredFlowCapped,
		double mostRemoteStandpipeFlowGpm,
		double flowAtSupplyGpm,
		double flowAddedAtSupplyGpm,
		boolean flowAddedAtSupplyAutomatic,
		double pressureAtSupplyPsi,
		double totalFrictionLossPsi,
		double totalElevationPsi,
		int requiredFdcInlets,
		InletSize fdcInletSize,
		boolean manualSystem,
		double supplyDurationMinutes,
		double waterSupplyVolumeGal,
		List<FdcLineRow> fdcLines,
		Double highestFdcPressurePsi,
		Double staticPressureAtLowestConnectionPsi,
		List<SegmentRow> segments,
		List<ComplianceCheck> checks) {

	/**
	 * One step of the demand build-up.
	 *
	 * @param clause      the clause the figure comes from
	 * @param description what it is
	 * @param flowGpm     the flow this step contributes or sets, gpm
	 */
	public record DemandLine(String clause, String description, double flowGpm) {

	}

	/**
	 * One fire department connection: the pressure the fire department must deliver at it to supply
	 * the flow it carries (A.10.4.1).
	 *
	 * @param label                the connection
	 * @param flowGpm              the flow it carries
	 * @param inlets               inlets given for it; null when not given
	 * @param segments             one worksheet row per pipe, system end first. In each row the "remote end" is the system
	 *                             side and the "supply end" the connection side
	 * @param totalFrictionLossPsi friction over its pipes
	 * @param totalElevationPsi    elevation pressure over its pipes
	 * @param pressureAtSystemPsi  pressure required at its system end, the supply end of the main path
	 * @param pressureAtFdcPsi     pressure required at the connection
	 */
	public record FdcLineRow(
			String label,
			double flowGpm,
			Integer inlets,
			List<SegmentRow> segments,
			double totalFrictionLossPsi,
			double totalElevationPsi,
			double pressureAtSystemPsi,
			double pressureAtFdcPsi) {

	}

	/**
	 * One worksheet row.
	 *
	 * @param label                   the segment
	 * @param role                    what the pipe is
	 * @param hoseConnectionSize      size of the hose connection at its remote end; null when none
	 * @param nominalSize             nominal size key
	 * @param insideDiameterInch      actual inside diameter, in.
	 * @param cFactor                 Hazen-Williams C
	 * @param flowAddedGpm            flow entering at the remote end
	 * @param flowGpm                 total flow in the pipe
	 * @param velocityFtPerSec        velocity, ft/s
	 * @param pipeLengthFt            pipe length
	 * @param fittingsLengthFt        equivalent length of the fittings, after the C and diameter adjustments
	 * @param frictionPsiPerFt        Hazen-Williams loss per foot
	 * @param frictionLossPsi         friction over the segment
	 * @param elevationRiseFt         rise toward the remote end
	 * @param elevationPsi            elevation pressure
	 * @param pressureAtRemoteEndPsi  pressure at the remote end of the segment, after any residual requirement
	 * @param pressureAtSupplyEndPsi  pressure required at the supply end of the segment
	 * @param residualRaised          whether the remote-end pressure was raised to the connection's minimum residual
	 * @param otherStandpipeConnection whether the flow added at the remote end is another standpipe's, at its connection point
	 */
	public record SegmentRow(
			String label,
			PipeRole role,
			HoseConnectionSize hoseConnectionSize,
			String nominalSize,
			double insideDiameterInch,
			double cFactor,
			double flowAddedGpm,
			double flowGpm,
			double velocityFtPerSec,
			double pipeLengthFt,
			double fittingsLengthFt,
			double frictionPsiPerFt,
			double frictionLossPsi,
			double elevationRiseFt,
			double elevationPsi,
			double pressureAtRemoteEndPsi,
			double pressureAtSupplyEndPsi,
			boolean residualRaised,
			boolean otherStandpipeConnection) {

	}

}
