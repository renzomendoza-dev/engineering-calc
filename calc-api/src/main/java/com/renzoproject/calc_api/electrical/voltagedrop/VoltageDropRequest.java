package com.renzoproject.calc_api.electrical.voltagedrop;

import com.renzoproject.calc.core.electrical.reference.ConductorMaterial;
import com.renzoproject.calc.core.electrical.reference.ConduitMaterial;
import com.renzoproject.calc.core.electrical.voltagedrop.CircuitType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * HTTP request body for a voltage drop calculation.
 *
 * <p>A plain mutable bean (rather than a record) so Jackson can leave
 * {@code parallelSetsPerPhase} at its field-initialized default of {@code 1} (and
 * {@code useCustomImpedance} at {@code false}) when the JSON body omits them.
 *
 * <p>{@code useCustomImpedance} is a mode switch, not two endpoints: when {@code false}
 * (the default), conductor R/X is resolved from PEC reference tables via
 * {@code conductorSizeLabel} / {@code conductorMaterial} / {@code conduitMaterial}; when
 * {@code true}, {@code customResistanceOhmsPerMeter} / {@code customReactanceOhmsPerMeter}
 * are used directly instead. {@link #isImpedanceSelectionValid()} enforces that exactly the
 * right fields are populated for whichever mode is selected.
 *
 * <p>{@code powerFactor} is only range-checked here ({@code [0, 1]}, so {@code 0} is accepted
 * for DC requests where it's ignored). The stricter AC-only rule — power factor must be in
 * {@code (0, 1]} — is left to {@code VoltageDropInput}'s own validation in calc-core, which
 * throws {@link com.renzoproject.calc.core.exception.CalculationException} on violation; that
 * is translated to a 400 response by {@code GlobalExceptionHandler}. This avoids a duplicate
 * cross-field validator here for a rule calc-core already enforces.
 */
public class VoltageDropRequest {

	@NotNull
	private CircuitType circuitType;

	@Positive
	private double currentAmps;

	@Positive
	private double oneWayLengthMeters;

	@DecimalMin(value = "0.0", inclusive = true)
	@DecimalMax(value = "1.0", inclusive = true)
	private double powerFactor;

	@Positive
	private double systemVoltage;

	@Min(1)
	private int parallelSetsPerPhase = 1;

	private boolean useCustomImpedance = false;

	private String conductorSizeLabel;

	private ConductorMaterial conductorMaterial;

	private ConduitMaterial conduitMaterial;

	@Positive
	private Double customResistanceOhmsPerMeter;

	@PositiveOrZero
	private Double customReactanceOhmsPerMeter;

	@AssertTrue(message = "When useCustomImpedance is true, customResistanceOhmsPerMeter "
			+ "(positive) and customReactanceOhmsPerMeter (zero or positive) are required. "
			+ "When false, conductorSizeLabel and conductorMaterial are required, plus "
			+ "conduitMaterial unless circuitType is DC.")
	public boolean isImpedanceSelectionValid() {
		if (useCustomImpedance) {
			return customResistanceOhmsPerMeter != null && customResistanceOhmsPerMeter > 0
					&& customReactanceOhmsPerMeter != null && customReactanceOhmsPerMeter >= 0;
		}
		boolean hasReferenceFields = conductorSizeLabel != null && conductorMaterial != null;
		if (circuitType != CircuitType.DC) {
			hasReferenceFields = hasReferenceFields && conduitMaterial != null;
		}
		return hasReferenceFields;
	}

	public CircuitType getCircuitType() {
		return circuitType;
	}

	public void setCircuitType(CircuitType circuitType) {
		this.circuitType = circuitType;
	}

	public double getCurrentAmps() {
		return currentAmps;
	}

	public void setCurrentAmps(double currentAmps) {
		this.currentAmps = currentAmps;
	}

	public double getOneWayLengthMeters() {
		return oneWayLengthMeters;
	}

	public void setOneWayLengthMeters(double oneWayLengthMeters) {
		this.oneWayLengthMeters = oneWayLengthMeters;
	}

	public double getPowerFactor() {
		return powerFactor;
	}

	public void setPowerFactor(double powerFactor) {
		this.powerFactor = powerFactor;
	}

	public double getSystemVoltage() {
		return systemVoltage;
	}

	public void setSystemVoltage(double systemVoltage) {
		this.systemVoltage = systemVoltage;
	}

	public int getParallelSetsPerPhase() {
		return parallelSetsPerPhase;
	}

	public void setParallelSetsPerPhase(int parallelSetsPerPhase) {
		this.parallelSetsPerPhase = parallelSetsPerPhase;
	}

	public boolean isUseCustomImpedance() {
		return useCustomImpedance;
	}

	public void setUseCustomImpedance(boolean useCustomImpedance) {
		this.useCustomImpedance = useCustomImpedance;
	}

	public String getConductorSizeLabel() {
		return conductorSizeLabel;
	}

	public void setConductorSizeLabel(String conductorSizeLabel) {
		this.conductorSizeLabel = conductorSizeLabel;
	}

	public ConductorMaterial getConductorMaterial() {
		return conductorMaterial;
	}

	public void setConductorMaterial(ConductorMaterial conductorMaterial) {
		this.conductorMaterial = conductorMaterial;
	}

	public ConduitMaterial getConduitMaterial() {
		return conduitMaterial;
	}

	public void setConduitMaterial(ConduitMaterial conduitMaterial) {
		this.conduitMaterial = conduitMaterial;
	}

	public Double getCustomResistanceOhmsPerMeter() {
		return customResistanceOhmsPerMeter;
	}

	public void setCustomResistanceOhmsPerMeter(Double customResistanceOhmsPerMeter) {
		this.customResistanceOhmsPerMeter = customResistanceOhmsPerMeter;
	}

	public Double getCustomReactanceOhmsPerMeter() {
		return customReactanceOhmsPerMeter;
	}

	public void setCustomReactanceOhmsPerMeter(Double customReactanceOhmsPerMeter) {
		this.customReactanceOhmsPerMeter = customReactanceOhmsPerMeter;
	}

}
