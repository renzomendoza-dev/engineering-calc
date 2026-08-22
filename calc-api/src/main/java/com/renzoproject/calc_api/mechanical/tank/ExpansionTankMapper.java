package com.renzoproject.calc_api.mechanical.tank;

import com.renzoproject.calc.core.mechanical.tank.ExpansionTankInput;
import com.renzoproject.calc.core.mechanical.tank.ExpansionTankResult;
import com.renzoproject.calc.core.mechanical.tank.SystemType;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.MetricPrefix;
import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Temperature;
import javax.measure.quantity.Volume;

/**
 * Pure mapping, no logic -- matches every other mapper in this codebase. Liters&lt;-&gt;m3
 * (volume), mm&lt;-&gt;m (pipe diameter), and kPa&lt;-&gt;Pa (pressure) boundary conversions
 * happen here: calc-core stays SI-pure internally, this mapper is the one place that knows this
 * endpoint's DTOs use liters/mm/kPa.
 *
 * <p>No dedicated liters&lt;-&gt;m3 helper exists elsewhere in the codebase to reuse -- neither
 * water storage mapper needs one ({@code DomesticWaterStorageMapper} reports its result directly
 * in m3, {@code FireWaterStorageMapper} reports in US gallons via {@code StorageUnits.GALLON_US})
 * -- so this mapper uses Indriya's {@code Units.LITRE} directly, same as {@code Units.CUBIC_METRE}
 * is used directly elsewhere: {@code Volume} is a standard JSR-385 quantity, no custom unit
 * definition needed.
 *
 * <p>{@code averagePipeDiameterMm} maps straight through as a plain double, not a
 * {@code Quantity<Length>} -- calc-core's {@code ExpansionTankInput} itself models this field as
 * a raw millimeter estimate (a secondary contributor to system volume, deliberately not the full
 * {@code DiameterSpec} precision), so there's nothing to convert.
 */
public final class ExpansionTankMapper {

	private static final double DEFAULT_ADDITIONAL_VOLUME_FACTOR = 1.0;
	private static final Unit<Pressure> KILOPASCAL = MetricPrefix.KILO(Units.PASCAL);

	private ExpansionTankMapper() {
	}

	public static ExpansionTankInput toCoreInput(ExpansionTankRequest request) {
		Quantity<Volume> primaryVesselVolume = Quantities.getQuantity(request.primaryVesselVolumeLiters(), Units.LITRE);
		Quantity<Length> estimatedPipingLength = Quantities.getQuantity(request.estimatedPipingLengthMeters(), Units.METRE);
		double additionalVolumeFactor = request.additionalVolumeFactor() == null
				? DEFAULT_ADDITIONAL_VOLUME_FACTOR
				: request.additionalVolumeFactor();
		Quantity<Temperature> coldWaterTemperature = Quantities.getQuantity(request.coldWaterTemperatureCelsius(), Units.CELSIUS);
		Quantity<Temperature> hotWaterTemperature = Quantities.getQuantity(request.hotWaterTemperatureCelsius(), Units.CELSIUS);
		Quantity<Pressure> fillPressureGauge = Quantities.getQuantity(request.fillPressureGaugeKpa(), KILOPASCAL);
		Quantity<Pressure> maxOperatingPressureGauge = Quantities.getQuantity(request.maxOperatingPressureGaugeKpa(), KILOPASCAL);
		Quantity<Length> altitude = Quantities.getQuantity(request.altitudeMeters(), Units.METRE);

		return new ExpansionTankInput(
				toCoreSystemType(request.systemType()),
				primaryVesselVolume,
				estimatedPipingLength,
				request.averagePipeDiameterMm(),
				additionalVolumeFactor,
				coldWaterTemperature,
				hotWaterTemperature,
				fillPressureGauge,
				maxOperatingPressureGauge,
				altitude);
	}

	public static ExpansionTankResponse toResponse(ExpansionTankResult result) {
		return new ExpansionTankResponse(
				result.derivedSystemVolume().to(Units.LITRE).getValue().doubleValue(),
				result.expansionVolume().to(Units.LITRE).getValue().doubleValue(),
				result.requiredTankVolume().to(Units.LITRE).getValue().doubleValue(),
				result.coldWaterDensityKgM3(),
				result.hotWaterDensityKgM3(),
				result.fillPressureAbsolute().to(KILOPASCAL).getValue().doubleValue(),
				result.maxOperatingPressureAbsolute().to(KILOPASCAL).getValue().doubleValue());
	}

	private static SystemType toCoreSystemType(SystemTypeDto dto) {
		return switch (dto) {
			case DOMESTIC_WATER_HEATER -> SystemType.DOMESTIC_WATER_HEATER;
			case HYDRONIC_HEATING -> SystemType.HYDRONIC_HEATING;
		};
	}

}
