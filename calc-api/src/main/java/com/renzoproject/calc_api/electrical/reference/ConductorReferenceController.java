package com.renzoproject.calc_api.electrical.reference;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/electrical/reference")
public class ConductorReferenceController {

	private final ConductorReferenceService service;

	public ConductorReferenceController(ConductorReferenceService service) {
		this.service = service;
	}

	@GetMapping("/conductor-sizes")
	public List<ConductorSizeDto> conductorSizes() {
		return service.listConductorSizes();
	}

	@GetMapping("/conductor-materials")
	public List<String> conductorMaterials() {
		return service.listConductorMaterials();
	}

	@GetMapping("/conduit-materials")
	public List<String> conduitMaterials() {
		return service.listConduitMaterials();
	}

	@GetMapping("/conductor-impedance-table")
	public List<ConductorImpedanceEntryDto> conductorImpedanceTable() {
		return service.listConductorImpedanceTable();
	}

	@GetMapping("/conductor-dc-resistance-table")
	public List<ConductorDcResistanceEntryDto> conductorDcResistanceTable() {
		return service.listConductorDcResistanceTable();
	}

	@GetMapping("/insulation-types")
	public List<String> insulationTypes() {
		return service.listInsulationTypes();
	}

	@GetMapping("/conduit-fill-types")
	public List<String> conduitFillTypes() {
		return service.listConduitFillTypes();
	}

	@GetMapping("/conductor-fill-sizes")
	public List<String> conductorFillSizes(@RequestParam String insulationType) {
		return service.listConductorFillSizes(insulationType);
	}

	@GetMapping("/conductor-dimension-table")
	public List<ConductorDimensionEntryDto> conductorDimensionTable() {
		return service.listConductorDimensionTable();
	}

	@GetMapping("/conduit-dimension-table")
	public List<ConduitDimensionEntryDto> conduitDimensionTable() {
		return service.listConduitDimensionTable();
	}

	@GetMapping("/termination-temp-ratings")
	public List<Integer> terminationTempRatings() {
		return service.listTerminationTempRatings();
	}

	@GetMapping("/motor-phase-types")
	public List<String> motorPhaseTypes() {
		return service.listMotorPhaseTypes();
	}

	@GetMapping("/motor-classes")
	public List<String> motorClasses() {
		return service.listMotorClasses();
	}

	@GetMapping("/motor-horsepower-ratings")
	public List<String> motorHorsepowerRatings(
			@RequestParam String phaseType,
			@RequestParam(required = false) String motorClass) {
		return service.listMotorHorsepowerRatings(phaseType, motorClass);
	}

	@GetMapping("/motor-voltages")
	public List<Integer> motorVoltages(
			@RequestParam String phaseType,
			@RequestParam(required = false) String motorClass,
			@RequestParam String horsepowerLabel) {
		return service.listMotorVoltages(phaseType, motorClass, horsepowerLabel);
	}

	@GetMapping("/motor-flc-table")
	public List<MotorFlcEntryDto> motorFlcTable() {
		return service.listMotorFlcTable();
	}

	@GetMapping("/locked-rotor-single-phase-table")
	public List<LockedRotorEntryDto> lockedRotorSinglePhaseTable() {
		return service.listLockedRotorSinglePhaseTable();
	}

	@GetMapping("/locked-rotor-polyphase-table")
	public List<LockedRotorEntryDto> lockedRotorPolyphaseTable() {
		return service.listLockedRotorPolyphaseTable();
	}

	@GetMapping("/ampacity-table")
	public List<AmpacityEntryDto> ampacityTable() {
		return service.listAmpacityTable();
	}

	@GetMapping("/insulation-type-temp-rating-table")
	public List<InsulationTypeTempRatingEntryDto> insulationTypeTempRatingTable() {
		return service.listInsulationTypeTempRatingTable();
	}

	@GetMapping("/ambient-temp-correction-table")
	public List<AmbientTempCorrectionEntryDto> ambientTempCorrectionTable() {
		return service.listAmbientTempCorrectionTable();
	}

	@GetMapping("/conductor-count-adjustment-table")
	public List<ConductorCountAdjustmentEntryDto> conductorCountAdjustmentTable() {
		return service.listConductorCountAdjustmentTable();
	}

}
