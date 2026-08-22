package com.renzoproject.calc.core.mechanical.storage;

import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;

import javax.measure.Quantity;
import javax.measure.quantity.Volume;

/**
 * Result of {@link DomesticWaterStorageCalculator}. {@code resolvedDemandFlowRate} is always
 * reported in SI (convertible to any compatible unit via {@link Quantity#to}), regardless of
 * which {@link DemandBasis} path produced it.
 *
 * @param resolvedDemandFlowRate resolved demand flow rate
 * @param requiredStorageVolume  {@code resolvedDemandFlowRate * storageDurationHours * (1 + safetyMarginPercent/100)}
 */
public record DomesticWaterStorageResult(
		Quantity<VolumetricFlowRate> resolvedDemandFlowRate,
		Quantity<Volume> requiredStorageVolume) {

}
