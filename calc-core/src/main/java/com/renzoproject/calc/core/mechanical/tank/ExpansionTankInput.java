package com.renzoproject.calc.core.mechanical.tank;

import com.renzoproject.calc.core.exception.CalculationException;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Temperature;
import javax.measure.quantity.Volume;

/**
 * Input for {@link ExpansionTankCalculator}.
 *
 * @param systemType              domestic water heater or hydronic heating
 * @param primaryVesselVolume     water heater tank volume, or boiler water content
 * @param estimatedPipingLength   secondary contributor to system volume; zero-length is valid for
 *                                a very compact system
 * @param averagePipeDiameterMm   plain numeric estimate, not a full standard-size resolver -- a
 *                                secondary contributor to system volume, not worth the extra
 *                                precision
 * @param additionalVolumeFactor  optional multiplier for fittings/terminal units not captured by
 *                                the simple cylinder estimate; pass {@code 1.0} for none
 * @param coldWaterTemperature    resolved via {@code FluidPropertiesResolver}
 * @param hotWaterTemperature     water heater setpoint, or hydronic max supply temperature;
 *                                resolved via {@code FluidPropertiesResolver}; must exceed
 *                                {@code coldWaterTemperature}
 * @param fillPressureGauge       tank/system fill (charge) pressure, gauge
 * @param maxOperatingPressureGauge maximum operating (relief-set) pressure, gauge; must exceed
 *                                {@code fillPressureGauge}
 * @param altitude                fed to {@code StandardAtmosphere} for gauge-to-absolute pressure
 *                                conversion
 * @throws CalculationException if any of the above rules is violated
 */
public record ExpansionTankInput(
		SystemType systemType,
		Quantity<Volume> primaryVesselVolume,
		Quantity<Length> estimatedPipingLength,
		double averagePipeDiameterMm,
		double additionalVolumeFactor,
		Quantity<Temperature> coldWaterTemperature,
		Quantity<Temperature> hotWaterTemperature,
		Quantity<Pressure> fillPressureGauge,
		Quantity<Pressure> maxOperatingPressureGauge,
		Quantity<Length> altitude) {

	public ExpansionTankInput {
		if (systemType == null) {
			throw new CalculationException("systemType is required");
		}
		if (primaryVesselVolume == null) {
			throw new CalculationException("primaryVesselVolume is required");
		}
		if (primaryVesselVolume.getValue().doubleValue() <= 0) {
			throw new CalculationException("primaryVesselVolume must be positive");
		}
		if (estimatedPipingLength == null) {
			throw new CalculationException("estimatedPipingLength is required");
		}
		if (estimatedPipingLength.getValue().doubleValue() < 0) {
			throw new CalculationException("estimatedPipingLength must not be negative");
		}
		if (averagePipeDiameterMm <= 0) {
			throw new CalculationException("averagePipeDiameterMm must be positive");
		}
		if (additionalVolumeFactor <= 0) {
			throw new CalculationException("additionalVolumeFactor must be positive");
		}
		if (coldWaterTemperature == null) {
			throw new CalculationException("coldWaterTemperature is required");
		}
		if (hotWaterTemperature == null) {
			throw new CalculationException("hotWaterTemperature is required");
		}
		if (hotWaterTemperature.to(Units.CELSIUS).getValue().doubleValue()
				<= coldWaterTemperature.to(Units.CELSIUS).getValue().doubleValue()) {
			throw new CalculationException("hotWaterTemperature must be greater than coldWaterTemperature "
					+ "(thermal expansion requires heating -- check for swapped inputs)");
		}
		if (fillPressureGauge == null) {
			throw new CalculationException("fillPressureGauge is required");
		}
		if (maxOperatingPressureGauge == null) {
			throw new CalculationException("maxOperatingPressureGauge is required");
		}
		// maxOperatingPressureAbsolute > fillPressureAbsolute reduces to this raw gauge comparison,
		// since both absolute pressures get the same altitude-derived atmospheric pressure added --
		// no need to resolve StandardAtmosphere just to validate.
		if (maxOperatingPressureGauge.to(Units.PASCAL).getValue().doubleValue()
				<= fillPressureGauge.to(Units.PASCAL).getValue().doubleValue()) {
			throw new CalculationException("maxOperatingPressureGauge must be greater than fillPressureGauge "
					+ "-- a tank can't be charged to operate at or below its own fill pressure");
		}
		if (altitude == null) {
			throw new CalculationException("altitude is required");
		}
	}

}
