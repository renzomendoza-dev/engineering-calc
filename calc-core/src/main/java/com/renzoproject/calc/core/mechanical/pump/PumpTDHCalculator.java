package com.renzoproject.calc.core.mechanical.pump;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.FluidProperties;
import com.renzoproject.calc.core.mechanical.pipe.FluidPropertiesResolver;
import com.renzoproject.calc.core.mechanical.pipe.PipePressureLossCalculator;
import com.renzoproject.calc.core.mechanical.pipe.PipePressureLossInput;
import com.renzoproject.calc.core.mechanical.pipe.PipePressureLossResult;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import java.util.ArrayList;
import java.util.List;

/**
 * Total Dynamic Head for a pump duty point: static head (elevation + suction condition) plus
 * suction/discharge friction loss plus residual pressure head plus an optional velocity head.
 *
 * <p>The first calculator in this codebase that calls another calculator internally —
 * {@link PipePressureLossCalculator} is constructor-injected (plain Java, no Spring in this
 * module) and does all the actual friction-loss math; this class never reimplements
 * Darcy-Weisbach itself, only orchestrates it once per pipe segment and sums the results.
 *
 * <p><b>A non-positive {@code totalDynamicHead} is not an error here.</b> Unlike
 * {@code FirePumpDemandCalculator} (which throws if its computed rated pressure comes out
 * non-positive, since that indicates a code-compliance design problem), a strongly gravity-fed
 * system can legitimately need near-zero or even negative head from the pump — that's a
 * plausible real answer for this calculation, not a sign of invalid input. This is surfaced as
 * {@link PumpTDHResult#staticallyFedWarning()} instead of a thrown exception.
 */
public class PumpTDHCalculator implements Calculator<PumpTDHInput, PumpTDHResult> {

	private static final double GRAVITY_M_S2 = 9.80665;

	private final PipePressureLossCalculator pressureLossCalculator;
	private final FluidPropertiesResolver fluidPropertiesResolver;

	public PumpTDHCalculator(PipePressureLossCalculator pressureLossCalculator, FluidPropertiesResolver fluidPropertiesResolver) {
		this.pressureLossCalculator = pressureLossCalculator;
		this.fluidPropertiesResolver = fluidPropertiesResolver;
	}

	@Override
	public PumpTDHResult calculate(PumpTDHInput input) {
		FluidProperties fluid = fluidPropertiesResolver.resolve(input.fluidKey(), input.fluidTemperature());

		double staticHeadM = staticHeadM(input);

		SegmentComputation suction = computeSegments(input, input.suctionSegments(), "suction");
		SegmentComputation discharge = computeSegments(input, input.dischargeSegments(), "discharge");

		double residualPressurePa = input.requiredResidualPressure().to(Units.PASCAL).getValue().doubleValue();
		double residualPressureHeadM = residualPressurePa / (fluid.densityKgM3() * GRAVITY_M_S2);

		double velocityHeadM = 0.0;
		if (input.includeVelocityHead()) {
			if (discharge.lastVelocityMs() == null) {
				throw new CalculationException("includeVelocityHead is true but dischargeSegments is empty -- "
						+ "there is no discharge velocity to compute a velocity head from");
			}
			velocityHeadM = (discharge.lastVelocityMs() * discharge.lastVelocityMs()) / (2 * GRAVITY_M_S2);
		}

		double totalDynamicHeadM = staticHeadM + suction.totalHeadLossM() + discharge.totalHeadLossM()
				+ residualPressureHeadM + velocityHeadM;
		boolean staticallyFedWarning = totalDynamicHeadM <= 0;

		return new PumpTDHResult(
				Quantities.getQuantity(staticHeadM, Units.METRE),
				Quantities.getQuantity(suction.totalHeadLossM(), Units.METRE),
				Quantities.getQuantity(discharge.totalHeadLossM(), Units.METRE),
				Quantities.getQuantity(residualPressureHeadM, Units.METRE),
				Quantities.getQuantity(velocityHeadM, Units.METRE),
				Quantities.getQuantity(totalDynamicHeadM, Units.METRE),
				staticallyFedWarning,
				suction.details(),
				discharge.details());
	}

	private static double staticHeadM(PumpTDHInput input) {
		double dischargeElevationM = input.staticDischargeElevation().to(Units.METRE).getValue().doubleValue();
		if (input.suctionCondition() == SuctionCondition.FLOODED) {
			double suctionHeadM = input.staticSuctionHeadFlooded().to(Units.METRE).getValue().doubleValue();
			return dischargeElevationM - suctionHeadM;
		}
		double liftM = input.staticSuctionLift().to(Units.METRE).getValue().doubleValue();
		return dischargeElevationM + liftM;
	}

	private SegmentComputation computeSegments(PumpTDHInput input, List<PipeSegmentSpec> segments, String sideLabel) {
		double totalHeadLossM = 0.0;
		List<SegmentLossDetail> details = new ArrayList<>();
		Double lastVelocityMs = null;

		for (PipeSegmentSpec segment : segments) {
			PipePressureLossInput segmentInput = new PipePressureLossInput(
					input.fluidKey(), input.fluidTemperature(), input.flowRate(),
					segment.diameterSpec(), segment.length(), segment.method());

			PipePressureLossResult result;
			try {
				result = pressureLossCalculator.calculate(segmentInput);
			} catch (CalculationException e) {
				throw new CalculationException("Pressure loss calculation failed for a " + sideLabel + " segment: " + e.getMessage(), e);
			}

			totalHeadLossM += result.headLoss().to(Units.METRE).getValue().doubleValue();
			details.add(new SegmentLossDetail(segment, result));
			lastVelocityMs = result.velocity().to(Units.METRE_PER_SECOND).getValue().doubleValue();
		}

		return new SegmentComputation(totalHeadLossM, List.copyOf(details), lastVelocityMs);
	}

	private record SegmentComputation(double totalHeadLossM, List<SegmentLossDetail> details, Double lastVelocityMs) {

	}

}
