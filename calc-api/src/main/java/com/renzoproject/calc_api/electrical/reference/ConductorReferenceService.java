package com.renzoproject.calc_api.electrical.reference;

import com.renzoproject.calc.core.electrical.reference.AmbientTempCorrectionTable;
import com.renzoproject.calc.core.electrical.reference.AmpacityTable;
import com.renzoproject.calc.core.electrical.reference.ConductorCountAdjustmentTable;
import com.renzoproject.calc.core.electrical.reference.ConductorDimensionTable;
import com.renzoproject.calc.core.electrical.reference.ConductorMaterial;
import com.renzoproject.calc.core.electrical.reference.ConductorPropertiesResolver;
import com.renzoproject.calc.core.electrical.reference.ConduitDimensionTable;
import com.renzoproject.calc.core.electrical.reference.ConduitMaterial;
import com.renzoproject.calc.core.electrical.reference.ConduitType;
import com.renzoproject.calc.core.electrical.reference.InsulationType;
import com.renzoproject.calc.core.electrical.reference.InsulationTypeTempRating;
import com.renzoproject.calc.core.electrical.reference.LockedRotorPolyphaseTable;
import com.renzoproject.calc.core.electrical.reference.LockedRotorSinglePhaseTable;
import com.renzoproject.calc.core.electrical.reference.MotorClass;
import com.renzoproject.calc.core.electrical.reference.MotorFlcTable;
import com.renzoproject.calc.core.electrical.reference.MotorPhaseType;
import com.renzoproject.calc.core.exception.CalculationException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Exposes calc-core's PEC reference data (conductor sizes, materials) for populating
 * frontend dropdowns.
 *
 * <p>{@link ConductorPropertiesResolver} is plainly instantiated, same pattern as
 * {@code VoltageDropService} — see that class's Javadoc for why.
 */
@Service
public class ConductorReferenceService {

	private final ConductorPropertiesResolver resolver = new ConductorPropertiesResolver();
	private final ConductorDimensionTable conductorDimensionTable = new ConductorDimensionTable();
	private final ConduitDimensionTable conduitDimensionTable = new ConduitDimensionTable();
	private final MotorFlcTable motorFlcTable = new MotorFlcTable();
	private final LockedRotorSinglePhaseTable lockedRotorSinglePhaseTable = new LockedRotorSinglePhaseTable();
	private final LockedRotorPolyphaseTable lockedRotorPolyphaseTable = new LockedRotorPolyphaseTable();
	private final AmpacityTable ampacityTable = new AmpacityTable();
	private final InsulationTypeTempRating insulationTypeTempRating = new InsulationTypeTempRating();
	private final AmbientTempCorrectionTable ambientTempCorrectionTable = new AmbientTempCorrectionTable();
	private final ConductorCountAdjustmentTable conductorCountAdjustmentTable = new ConductorCountAdjustmentTable();

	public List<ConductorSizeDto> listConductorSizes() {
		return resolver.allSizes().stream()
				.map(ConductorSizeDto::from)
				.toList();
	}

	public List<String> listConductorMaterials() {
		return Arrays.stream(ConductorMaterial.values()).map(Enum::name).toList();
	}

	public List<String> listConduitMaterials() {
		return Arrays.stream(ConduitMaterial.values()).map(Enum::name).toList();
	}

	public List<ConductorImpedanceEntryDto> listConductorImpedanceTable() {
		return resolver.allImpedanceEntries().stream()
				.map(ConductorImpedanceEntryDto::from)
				.toList();
	}

	public List<ConductorDcResistanceEntryDto> listConductorDcResistanceTable() {
		return resolver.allDcResistanceEntries().stream()
				.map(ConductorDcResistanceEntryDto::from)
				.toList();
	}

	/**
	 * Conduit fill's insulation types (calc-core's {@code InsulationType}) — distinct from
	 * voltage drop's {@link ConductorMaterial}.
	 */
	public List<String> listInsulationTypes() {
		return Arrays.stream(InsulationType.values()).map(InsulationType::toLabel).toList();
	}

	/**
	 * Conduit fill's conduit types (calc-core's {@code ConduitType}) — distinct from voltage
	 * drop's {@link ConduitMaterial}; served under a differently-named route
	 * ({@code conduit-fill-types}, not {@code conduit-types}) to keep that distinction obvious.
	 */
	public List<String> listConduitFillTypes() {
		return Arrays.stream(ConduitType.values()).map(ConduitType::toLabel).toList();
	}

	/**
	 * Conductor sizes with published dimension data for a given insulation type, for conduit
	 * fill's dropdowns.
	 *
	 * @throws com.renzoproject.calc.core.exception.CalculationException if insulationTypeLabel
	 *                                                                   doesn't match a known
	 *                                                                   {@code InsulationType}
	 */
	public List<String> listConductorFillSizes(String insulationTypeLabel) {
		InsulationType type = InsulationType.fromLabel(insulationTypeLabel);
		return conductorDimensionTable.sizeLabelsFor(type);
	}

	/** Raw rows of PEC Table 10.1.1.5 (insulated conductor dimensions), for display purposes. */
	public List<ConductorDimensionEntryDto> listConductorDimensionTable() {
		return conductorDimensionTable.allEntries().stream()
				.map(ConductorDimensionEntryDto::from)
				.toList();
	}

	/** Raw rows of PEC Table 10.1.1.4 (conduit/tubing dimensions), for display purposes. */
	public List<ConduitDimensionEntryDto> listConduitDimensionTable() {
		return conduitDimensionTable.allEntries().stream()
				.map(ConduitDimensionEntryDto::from)
				.toList();
	}

	/**
	 * Fixed at exactly {60, 75, 90} by definition — PEC's three standard termination
	 * temperature ratings. Unlike the other reference endpoints, this isn't sourced from a
	 * calc-core table (there's no table to load; wire sizing's own validation already enforces
	 * this exact set), so it's hardcoded here rather than exposing a loader for a 3-element
	 * constant. Not an oversight.
	 */
	public List<Integer> listTerminationTempRatings() {
		return List.of(60, 75, 90);
	}

	public List<String> listMotorPhaseTypes() {
		return Arrays.stream(MotorPhaseType.values()).map(Enum::name).toList();
	}

	public List<String> listMotorClasses() {
		return Arrays.stream(MotorClass.values()).map(Enum::name).toList();
	}

	/**
	 * Horsepower labels published for a phase type / motor class, for motor FLC's dropdowns.
	 *
	 * @throws CalculationException if phaseTypeLabel doesn't match a known {@code MotorPhaseType},
	 *                              or if motorClassLabel is missing or doesn't match a known
	 *                              {@code MotorClass} when phaseType is THREE_PHASE
	 */
	public List<String> listMotorHorsepowerRatings(String phaseTypeLabel, String motorClassLabel) {
		MotorPhaseType phaseType = parseEnum(MotorPhaseType.class, phaseTypeLabel, "motor phase type");
		MotorClass motorClass = null;
		if (phaseType == MotorPhaseType.THREE_PHASE) {
			if (motorClassLabel == null) {
				throw new CalculationException("motorClass is required when phaseType is THREE_PHASE");
			}
			motorClass = parseEnum(MotorClass.class, motorClassLabel, "motor class");
		}
		return motorFlcTable.sizeLabelsFor(phaseType, motorClass);
	}

	/**
	 * Voltages published for a phase type / motor class / horsepower label, for motor FLC's
	 * dropdowns. Same phaseType/motorClass validation as {@link #listMotorHorsepowerRatings}.
	 */
	public List<Integer> listMotorVoltages(String phaseTypeLabel, String motorClassLabel, String horsepowerLabel) {
		MotorPhaseType phaseType = parseEnum(MotorPhaseType.class, phaseTypeLabel, "motor phase type");
		MotorClass motorClass = null;
		if (phaseType == MotorPhaseType.THREE_PHASE) {
			if (motorClassLabel == null) {
				throw new CalculationException("motorClass is required when phaseType is THREE_PHASE");
			}
			motorClass = parseEnum(MotorClass.class, motorClassLabel, "motor class");
		}
		return motorFlcTable.voltagesFor(phaseType, motorClass, horsepowerLabel);
	}

	/**
	 * Raw rows of the combined PEC Article 4.30 full-load current tables (4.30.14.1 through
	 * 4.30.14.4), for display purposes.
	 */
	public List<MotorFlcEntryDto> listMotorFlcTable() {
		return motorFlcTable.allEntries().stream()
				.map(MotorFlcEntryDto::from)
				.toList();
	}

	/** Raw rows of PEC Table 4.30.14.5(A) (single-phase locked-rotor current), for display purposes. */
	public List<LockedRotorEntryDto> listLockedRotorSinglePhaseTable() {
		return lockedRotorSinglePhaseTable.allEntries().stream()
				.map(LockedRotorEntryDto::from)
				.toList();
	}

	/** Raw rows of PEC Table 4.30.14.5(B) (polyphase locked-rotor current), for display purposes. */
	public List<LockedRotorEntryDto> listLockedRotorPolyphaseTable() {
		return lockedRotorPolyphaseTable.allEntries().stream()
				.map(LockedRotorEntryDto::from)
				.toList();
	}

	/** Raw rows of PEC Table 3.10.2.6(B)(16) (base ampacities), for display purposes. */
	public List<AmpacityEntryDto> listAmpacityTable() {
		return ampacityTable.allEntries().stream()
				.map(AmpacityEntryDto::from)
				.toList();
	}

	/**
	 * Raw rows of the insulation-type-to-temperature-rating mapping (which
	 * {@link #listAmpacityTable} column each insulation type + material uses), for display
	 * purposes.
	 */
	public List<InsulationTypeTempRatingEntryDto> listInsulationTypeTempRatingTable() {
		return insulationTypeTempRating.allEntries().stream()
				.map(InsulationTypeTempRatingEntryDto::from)
				.toList();
	}

	/**
	 * Raw rows of PEC Table 3.10.2.6(B)(2)(a) (ambient temperature correction factors), for
	 * display purposes.
	 */
	public List<AmbientTempCorrectionEntryDto> listAmbientTempCorrectionTable() {
		return ambientTempCorrectionTable.allEntries().stream()
				.map(AmbientTempCorrectionEntryDto::from)
				.toList();
	}

	/**
	 * Raw rows of PEC Table 3.10.2.6(B)(3)(a) (ampacity adjustment for more than three
	 * current-carrying conductors), for display purposes.
	 */
	public List<ConductorCountAdjustmentEntryDto> listConductorCountAdjustmentTable() {
		return conductorCountAdjustmentTable.allEntries().stream()
				.map(ConductorCountAdjustmentEntryDto::from)
				.toList();
	}

	private static <E extends Enum<E>> E parseEnum(Class<E> enumType, String rawValue, String fieldLabel) {
		try {
			return Enum.valueOf(enumType, rawValue);
		} catch (IllegalArgumentException e) {
			throw new CalculationException("Unknown " + fieldLabel + ": " + rawValue);
		}
	}

}
