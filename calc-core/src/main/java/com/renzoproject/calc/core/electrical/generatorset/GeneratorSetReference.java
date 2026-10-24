package com.renzoproject.calc.core.electrical.generatorset;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzoproject.calc.core.exception.CalculationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Loads the reference data behind generator set sizing from the classpath once: the PEC
 * Article 2.20 lighting and receptacle tables, the scalar load and generator rules, the
 * Chapter 7 requirements per standby class, and the (non-PEC) standard generator sizes.
 *
 * <p>Plainly instantiated like the other table-backed calculators — calc-core has no Spring
 * dependency, so there's no DI to reach for here.
 */
public class GeneratorSetReference {

	private static final String BASE = "/reference/electrical/generator-set/";

	private final LightingFile lighting = load("lighting-occupancies.json", LightingFile.class);
	private final RulesFile rules = load("generator-set-rules.json", RulesFile.class);
	private final SizesFile sizes = load("standard-generator-sizes.json", SizesFile.class);
	private final StandbyFile standby = load("standby-class-requirements.json", StandbyFile.class);

	private static <T> T load(String name, Class<T> type) {
		try (InputStream in = GeneratorSetReference.class.getResourceAsStream(BASE + name)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + BASE + name);
			}
			return new ObjectMapper().readValue(in, type);
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + BASE + name, e);
		}
	}

	/** A band of a stepped demand factor: {@code percent} applies up to the cumulative {@code upToVoltAmperes}. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record DemandTier(Double upToVoltAmperes, double percent) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record DemandGroup(String key, String label, boolean fullDemandWhenAllLightingInUse, List<DemandTier> tiers) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Occupancy(String key, String label, double voltAmperesPerM2, String demandGroup, String note) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record AreaType(String key, String label, Double voltAmperesPerM2) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Requirement(String citation, String text) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record StandbyClassEntry(String key, String label, String capacityCitation, String capacityBasis, List<Requirement> requirements) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record TransferEntry(String key, String label, String citation, String text) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record LightingFile(String confidence, List<DemandGroup> demandGroups, List<Occupancy> occupancies, List<AreaType> areaTypes) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record RulesFile(
			double receptacleVoltAmperesPerOutlet,
			double bankOfficeReceptacleVoltAmperesPerM2,
			List<DemandTier> receptacleDemandTiers,
			double largestMotorFactor,
			double continuousLoadFactor,
			double generatorOutputFactor,
			double generatorOutputFactorWhereOverloadPrevented,
			double defaultGeneratorPowerFactor,
			String defaultGeneratorPowerFactorNote) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record SizesFile(String confidence, String note, List<Double> sizesKva) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record StandbyFile(List<StandbyClassEntry> classes, List<TransferEntry> optionalTransfer) {
	}

	public List<DemandGroup> demandGroups() {
		return lighting.demandGroups();
	}

	public List<Occupancy> occupancies() {
		return lighting.occupancies();
	}

	public List<AreaType> areaTypes() {
		return lighting.areaTypes();
	}

	public Occupancy occupancy(String key) {
		return lighting.occupancies().stream()
				.filter(entry -> entry.key().equals(key))
				.findFirst()
				.orElseThrow(() -> new CalculationException("Unknown lighting occupancy: " + key));
	}

	public AreaType areaType(String key) {
		return lighting.areaTypes().stream()
				.filter(entry -> entry.key().equals(key))
				.findFirst()
				.orElseThrow(() -> new CalculationException("Unknown lighting area type: " + key));
	}

	public DemandGroup demandGroup(String key) {
		return lighting.demandGroups().stream()
				.filter(entry -> entry.key().equals(key))
				.findFirst()
				.orElseThrow(() -> new CalculationException("Unknown lighting demand group: " + key));
	}

	public double receptacleVoltAmperesPerOutlet() {
		return rules.receptacleVoltAmperesPerOutlet();
	}

	public double bankOfficeReceptacleVoltAmperesPerM2() {
		return rules.bankOfficeReceptacleVoltAmperesPerM2();
	}

	public List<DemandTier> receptacleDemandTiers() {
		return rules.receptacleDemandTiers();
	}

	public double largestMotorFactor() {
		return rules.largestMotorFactor();
	}

	public double continuousLoadFactor() {
		return rules.continuousLoadFactor();
	}

	public double generatorOutputFactor() {
		return rules.generatorOutputFactor();
	}

	public double generatorOutputFactorWhereOverloadPrevented() {
		return rules.generatorOutputFactorWhereOverloadPrevented();
	}

	public double defaultGeneratorPowerFactor() {
		return rules.defaultGeneratorPowerFactor();
	}

	public String defaultGeneratorPowerFactorNote() {
		return rules.defaultGeneratorPowerFactorNote();
	}

	/** Standard generator set sizes in kVA, ascending. Not PEC data — see the file's own note. */
	public List<Double> standardSizesKva() {
		return sizes.sizesKva().stream().sorted().toList();
	}

	public String standardSizesNote() {
		return sizes.note();
	}

	public List<StandbyClassEntry> standbyClasses() {
		return standby.classes();
	}

	public StandbyClassEntry standbyClass(StandbyClass standbyClass) {
		return standby.classes().stream()
				.filter(entry -> entry.key().equals(standbyClass.name()))
				.findFirst()
				.orElseThrow(() -> new CalculationException("No requirements data for standby class " + standbyClass));
	}

	public List<TransferEntry> optionalTransfers() {
		return standby.optionalTransfer();
	}

	public TransferEntry optionalTransfer(OptionalStandbyTransfer transfer) {
		return standby.optionalTransfer().stream()
				.filter(entry -> entry.key().equals(transfer.name()))
				.findFirst()
				.orElseThrow(() -> new CalculationException("No data for optional standby transfer " + transfer));
	}

}
