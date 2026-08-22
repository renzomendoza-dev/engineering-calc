package com.renzoproject.calc.core.mechanical.duct;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.FrictionFactorMethod;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Speed;
import javax.measure.quantity.Temperature;

/**
 * Input for {@link DuctSizingCalculator}. Which of {@code fixedDimensionType}/
 * {@code fixedDimensionValue}/{@code targetFrictionRatePerMeter}/{@code maxVelocity} are required
 * depends on {@link #shape()}/{@link #method()} -- see the per-field notes below.
 *
 * @param method                      which sizing method to use
 * @param airFlow                     must be positive
 * @param airTemperature              fed to {@link AirPropertiesResolver}
 * @param altitude                    fed to {@link AirPropertiesResolver}
 * @param ductMaterial                resolved via {@link DuctRoughnessResolver}
 * @param frictionMethod              reused from {@code mechanical.pipe} -- needed regardless of
 *                                    {@code method}, since the result always reports Reynolds
 *                                    number/friction factor/actual friction rate
 * @param shape                       round or rectangular
 * @param fixedDimensionType          required for {@link DuctShape#RECTANGULAR}; ignored
 *                                    otherwise
 * @param fixedDimensionValue         required (and must be positive) for
 *                                    {@link DuctShape#RECTANGULAR}; ignored otherwise
 * @param targetFrictionRatePerMeter  required (and must be positive) for
 *                                    {@link DuctSizingMethod#EQUAL_FRICTION}; ignored otherwise
 * @param maxVelocity                 required (and must be positive) for
 *                                    {@link DuctSizingMethod#VELOCITY}; ignored otherwise
 * @throws CalculationException if any of the above rules is violated
 */
public record DuctSizingInput(
		DuctSizingMethod method,
		Quantity<VolumetricFlowRate> airFlow,
		Quantity<Temperature> airTemperature,
		Quantity<Length> altitude,
		String ductMaterial,
		FrictionFactorMethod frictionMethod,
		DuctShape shape,
		FixedDimensionType fixedDimensionType,
		Quantity<Length> fixedDimensionValue,
		Quantity<Pressure> targetFrictionRatePerMeter,
		Quantity<Speed> maxVelocity) {

	public DuctSizingInput {
		if (method == null) {
			throw new CalculationException("method is required");
		}
		if (airFlow == null) {
			throw new CalculationException("airFlow is required");
		}
		if (airFlow.getValue().doubleValue() <= 0) {
			throw new CalculationException("airFlow must be positive");
		}
		if (airTemperature == null) {
			throw new CalculationException("airTemperature is required");
		}
		if (altitude == null) {
			throw new CalculationException("altitude is required");
		}
		if (ductMaterial == null || ductMaterial.isBlank()) {
			throw new CalculationException("ductMaterial is required");
		}
		if (frictionMethod == null) {
			throw new CalculationException("frictionMethod is required");
		}
		if (shape == null) {
			throw new CalculationException("shape is required");
		}
		if (shape == DuctShape.RECTANGULAR) {
			if (fixedDimensionType == null) {
				throw new CalculationException("fixedDimensionType is required for RECTANGULAR shape");
			}
			if (fixedDimensionValue == null || fixedDimensionValue.getValue().doubleValue() <= 0) {
				throw new CalculationException("fixedDimensionValue must be positive for RECTANGULAR shape");
			}
		}
		if (method == DuctSizingMethod.EQUAL_FRICTION) {
			if (targetFrictionRatePerMeter == null || targetFrictionRatePerMeter.getValue().doubleValue() <= 0) {
				throw new CalculationException("targetFrictionRatePerMeter must be positive for EQUAL_FRICTION method");
			}
		}
		if (method == DuctSizingMethod.VELOCITY) {
			if (maxVelocity == null || maxVelocity.getValue().doubleValue() <= 0) {
				throw new CalculationException("maxVelocity must be positive for VELOCITY method");
			}
		}
	}

}
