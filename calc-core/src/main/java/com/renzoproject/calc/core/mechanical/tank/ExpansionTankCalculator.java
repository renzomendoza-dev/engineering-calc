package com.renzoproject.calc.core.mechanical.tank;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.mechanical.BoyleTankSizing;
import com.renzoproject.calc.core.mechanical.StandardAtmosphere;
import com.renzoproject.calc.core.mechanical.pipe.FluidProperties;
import com.renzoproject.calc.core.mechanical.pipe.FluidPropertiesResolver;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Volume;

/**
 * ASPE/ASHRAE diaphragm/bladder-type thermal expansion tank sizing, for domestic water heater or
 * hydronic heating systems. See {@code package-info.java} for the shared-utility reasoning.
 */
public class ExpansionTankCalculator implements Calculator<ExpansionTankInput, ExpansionTankResult> {

	private static final String WATER_FLUID_KEY = "WATER";

	private final FluidPropertiesResolver fluidPropertiesResolver;

	public ExpansionTankCalculator(FluidPropertiesResolver fluidPropertiesResolver) {
		this.fluidPropertiesResolver = fluidPropertiesResolver;
	}

	@Override
	public ExpansionTankResult calculate(ExpansionTankInput input) {
		double primaryVesselVolumeM3 = input.primaryVesselVolume().to(Units.CUBIC_METRE).getValue().doubleValue();
		double pipingVolumeM3 = pipingVolumeM3(input);
		double derivedSystemVolumeM3 = (primaryVesselVolumeM3 + pipingVolumeM3) * input.additionalVolumeFactor();

		FluidProperties coldWater = fluidPropertiesResolver.resolve(WATER_FLUID_KEY, input.coldWaterTemperature());
		FluidProperties hotWater = fluidPropertiesResolver.resolve(WATER_FLUID_KEY, input.hotWaterTemperature());

		double expansionVolumeM3 = derivedSystemVolumeM3 * (coldWater.densityKgM3() / hotWater.densityKgM3() - 1.0);
		Quantity<Volume> expansionVolume = Quantities.getQuantity(expansionVolumeM3, Units.CUBIC_METRE);

		Quantity<Pressure> atmosphericPressure = StandardAtmosphere.pressureAtAltitude(input.altitude());
		Quantity<Pressure> fillPressureAbsolute = input.fillPressureGauge().add(atmosphericPressure);
		Quantity<Pressure> maxOperatingPressureAbsolute = input.maxOperatingPressureGauge().add(atmosphericPressure);

		Quantity<Volume> requiredTankVolume =
				BoyleTankSizing.requiredVolume(expansionVolume, fillPressureAbsolute, maxOperatingPressureAbsolute);

		return new ExpansionTankResult(
				Quantities.getQuantity(derivedSystemVolumeM3, Units.CUBIC_METRE),
				expansionVolume,
				requiredTankVolume,
				coldWater.densityKgM3(),
				hotWater.densityKgM3(),
				fillPressureAbsolute,
				maxOperatingPressureAbsolute);
	}

	private static double pipingVolumeM3(ExpansionTankInput input) {
		double diameterM = input.averagePipeDiameterMm() / 1000.0;
		double lengthM = input.estimatedPipingLength().to(Units.METRE).getValue().doubleValue();
		return (Math.PI / 4.0) * diameterM * diameterM * lengthM;
	}

}
