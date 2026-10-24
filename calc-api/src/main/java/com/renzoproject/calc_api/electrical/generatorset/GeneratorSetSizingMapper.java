package com.renzoproject.calc_api.electrical.generatorset;

import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput;
import com.renzoproject.calc.core.electrical.generatorset.DirectLoadUnit;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.DirectLoad;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.FirePump;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.LightingArea;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.PumpMotor;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.SupplyRun;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.MotorLoad;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.OtherLoad;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.OutputConductor;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.ReceptacleGroup;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingResult;
import com.renzoproject.calc.core.electrical.generatorset.OptionalStandbyTransfer;
import com.renzoproject.calc.core.electrical.generatorset.StandbyClass;
import com.renzoproject.calc.core.electrical.reference.ConductorMaterial;
import com.renzoproject.calc.core.electrical.reference.ConduitMaterial;
import com.renzoproject.calc.core.electrical.reference.InsulationType;
import com.renzoproject.calc.core.electrical.reference.MotorPhaseType;
import com.renzoproject.calc_api.common.EnumParsing;

import java.util.List;

/**
 * Maps between calc-api's generator set sizing DTOs and calc-core's calculator types.
 */
public final class GeneratorSetSizingMapper {

	private GeneratorSetSizingMapper() {
	}

	public static GeneratorSetSizingInput toInput(GeneratorSetSizingRequest request) {
		StandbyClass standbyClass = EnumParsing.parse(StandbyClass.class, request.standbyClass(), "standby class");
		OptionalStandbyTransfer transfer = request.optionalTransfer() == null || request.optionalTransfer().isBlank()
				? null
				: EnumParsing.parse(OptionalStandbyTransfer.class, request.optionalTransfer(), "optional standby transfer");
		MotorPhaseType phaseType = EnumParsing.parse(MotorPhaseType.class, request.phaseType(), "phase type");

		return new GeneratorSetSizingInput(
				standbyClass,
				transfer,
				phaseType,
				request.voltage(),
				mapAll(request.lightingAreas(), area -> new LightingArea(
						area.label(), area.occupancy(), area.areaType(), area.floorAreaM2(),
						area.unitLoadVoltAmperesPerM2(), area.entireLightingUsedAtOnce(), area.continuous())),
				mapAll(request.receptacleGroups(), group -> new ReceptacleGroup(
						group.label(), group.outlets(), group.bankOrOfficeFloorAreaM2(), group.continuous())),
				request.applyReceptacleDemandFactor(),
				mapAll(request.otherLoads(), load -> new OtherLoad(
						load.label(), load.voltAmperes(), load.demandFactorPercent(), load.continuous())),
				mapAll(request.motors(), motor -> new MotorLoad(
						motor.label(), motor.horsepowerLabel(), motor.voltage(), motor.quantity(),
						motor.startStep() == null ? 1 : motor.startStep(),
						motor.startingCurrentPercent() == null ? 100.0 : motor.startingCurrentPercent())),
				request.generatorPowerFactor(),
				request.spareCapacityPercent(),
				request.selectedGeneratorKva(),
				request.designPreventsOverload(),
				request.outputConductor() == null ? null : toOutputConductor(request.outputConductor()),
				request.subtransientReactancePercent(),
				request.maximumVoltageDipPercent(),
				mapAll(request.directLoads(), GeneratorSetSizingMapper::toDirectLoad),
				toFirePump(request.firePump()));
	}

	private static DirectLoad toDirectLoad(DirectLoadDto dto) {
		return new DirectLoad(
				dto.label(),
				dto.amount(),
				EnumParsing.parse(DirectLoadUnit.class, dto.unit(), "total load unit"),
				dto.powerFactor(),
				dto.continuous());
	}

	private static PumpMotor toPumpMotor(PumpMotorDto dto) {
		return dto == null ? null : new PumpMotor(dto.horsepowerLabel(), dto.voltage(), dto.fullLoadAmps(), dto.lockedRotorAmps());
	}

	private static FirePump toFirePump(FirePumpDto dto) {
		if (dto == null) {
			return null;
		}
		SupplyRunDto run = dto.supplyRun();
		SupplyRun supplyRun = run == null ? null : new SupplyRun(
				run.ambientTempCelsius(),
				run.numberOfCurrentCarryingConductors(),
				run.numberOfParallelSets(),
				InsulationType.fromLabel(run.insulationType()),
				EnumParsing.parse(ConductorMaterial.class, run.conductorMaterial(), "conductor material"),
				run.terminationTempRatingCelsius(),
				run.oneWayLengthMeters(),
				run.conduitMaterial() == null || run.conduitMaterial().isBlank()
						? null
						: EnumParsing.parse(ConduitMaterial.class, run.conduitMaterial(), "conduit material"),
				run.startingPowerFactor(),
				run.runningPowerFactor(),
				run.cableStartingDropAllowancePercent());
		return new FirePump(
				toPumpMotor(dto.motor()),
				toPumpMotor(dto.jockey()),
				dto.accessoryAmps(),
				dto.startStep() == null ? 1 : dto.startStep(),
				dto.startingCurrentPercent() == null ? 100.0 : dto.startingCurrentPercent(),
				supplyRun);
	}

	private static OutputConductor toOutputConductor(OutputConductorDto dto) {
		return new OutputConductor(
				dto.ambientTempCelsius(),
				dto.numberOfCurrentCarryingConductors(),
				dto.numberOfParallelSets(),
				InsulationType.fromLabel(dto.insulationType()),
				EnumParsing.parse(ConductorMaterial.class, dto.conductorMaterial(), "conductor material"),
				dto.terminationTempRatingCelsius());
	}

	private static <D, C> List<C> mapAll(List<D> items, java.util.function.Function<D, C> mapper) {
		return items == null ? List.of() : items.stream().map(mapper).toList();
	}

	public static GeneratorSetSizingResponse toResponse(GeneratorSetSizingResult result) {
		return GeneratorSetSizingResponse.from(result);
	}

}
