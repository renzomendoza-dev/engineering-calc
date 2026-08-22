package com.renzoproject.calc.core.mechanical.storage;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.mechanical.pipe.PipeUnits;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Volume;

/**
 * NPC-based domestic water storage sizing, SI units throughout -- unlike
 * {@link FireWaterStorageCalculator}, which is deliberately GPM-native. Two independent demand
 * paths ({@link DemandBasis}), selected by the input; both feed the same
 * {@code flowRate * durationHours * (1 + safetyMargin)} volume formula.
 */
public class DomesticWaterStorageCalculator implements Calculator<DomesticWaterStorageInput, DomesticWaterStorageResult> {

	private static final double HOURS_PER_DAY = 24.0;
	private static final double SECONDS_PER_HOUR = 3600.0;

	private final PerCapitaConsumptionResolver perCapitaConsumptionResolver;
	private final FixtureUnitDemandResolver fixtureUnitDemandResolver;

	public DomesticWaterStorageCalculator(
			PerCapitaConsumptionResolver perCapitaConsumptionResolver,
			FixtureUnitDemandResolver fixtureUnitDemandResolver) {
		this.perCapitaConsumptionResolver = perCapitaConsumptionResolver;
		this.fixtureUnitDemandResolver = fixtureUnitDemandResolver;
	}

	@Override
	public DomesticWaterStorageResult calculate(DomesticWaterStorageInput input) {
		Quantity<VolumetricFlowRate> resolvedDemandFlowRate = switch (input.demandBasis()) {
			case OCCUPANT_LOAD -> resolveOccupantLoadFlowRate(input);
			case FIXTURE_UNIT -> resolveFixtureUnitFlowRate(input);
		};

		double flowRateLitresPerSecond = resolvedDemandFlowRate.to(PipeUnits.LITRE_PER_SECOND).getValue().doubleValue();
		double storageSeconds = input.storageDurationHours() * SECONDS_PER_HOUR;
		double volumeLitres = flowRateLitresPerSecond * storageSeconds * (1 + input.safetyMarginPercent() / 100.0);

		return new DomesticWaterStorageResult(resolvedDemandFlowRate, Quantities.getQuantity(volumeLitres, Units.LITRE));
	}

	/**
	 * Daily-average-as-hourly-rate simplification: {@code lpcd} is a per-capita PER-DAY figure,
	 * spread evenly across 24 hours to get a flow rate. This is NOT a peak-demand figure the way
	 * {@link #resolveFixtureUnitFlowRate} is -- it assumes uniform draw over the full day, which
	 * real domestic demand never actually is (mornings/evenings peak). Combined with
	 * {@link PerCapitaConsumptionResolver}'s placeholder-confidence data, treat
	 * {@code OCCUPANT_LOAD} results as a rough sizing estimate, not a hydraulic design figure.
	 */
	private Quantity<VolumetricFlowRate> resolveOccupantLoadFlowRate(DomesticWaterStorageInput input) {
		double lpcd = perCapitaConsumptionResolver.resolveLpcd(input.occupancyType());
		double dailyDemandLitres = input.occupantCount() * lpcd;
		double flowRateLitresPerSecond = dailyDemandLitres / (HOURS_PER_DAY * SECONDS_PER_HOUR);
		return Quantities.getQuantity(flowRateLitresPerSecond, PipeUnits.LITRE_PER_SECOND);
	}

	private Quantity<VolumetricFlowRate> resolveFixtureUnitFlowRate(DomesticWaterStorageInput input) {
		double gpm = fixtureUnitDemandResolver.resolveGpm(input.totalFixtureUnits(), input.systemType());
		return Quantities.getQuantity(gpm, PipeUnits.GALLON_US_PER_MINUTE).to(PipeUnits.LITRE_PER_SECOND);
	}

}
