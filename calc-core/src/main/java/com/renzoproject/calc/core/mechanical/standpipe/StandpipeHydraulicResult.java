package com.renzoproject.calc.core.mechanical.standpipe;

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
 * @param requiredFdcInlets     2.5 in. fire department connection inlets, one per 250 gpm (10.7.3)
 * @param supplyDurationMinutes duration used for the volume
 * @param waterSupplyVolumeGal  {@code flowAtSupply x duration}
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
		double supplyDurationMinutes,
		double waterSupplyVolumeGal,
		List<SegmentRow> segments,
		List<StandpipeCheck> checks) {

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
			boolean residualRaised) {

	}

}
