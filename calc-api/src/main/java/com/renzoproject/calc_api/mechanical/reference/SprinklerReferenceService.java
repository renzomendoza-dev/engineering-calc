package com.renzoproject.calc_api.mechanical.reference;

import com.renzoproject.calc.core.mechanical.sprinkler.CFactorResolver;
import com.renzoproject.calc.core.mechanical.sprinkler.FittingEquivalentLengthResolver;
import com.renzoproject.calc.core.mechanical.sprinkler.HazardPresetResolver;
import com.renzoproject.calc.core.mechanical.sprinkler.SprinklerKFactorResolver;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Exposes calc-core's sprinkler reference tables for populating a hydraulic calculation form:
 * hazard presets, Hazen-Williams C factors, fitting equivalent lengths and K-factors.
 *
 * <p>Pipe sizes with inside diameters are deliberately NOT re-exposed here --
 * {@code PipeReferenceService} already publishes the full material/schedule/size catalog at
 * {@code GET /api/mechanical/reference/pipe-materials}, and the sprinkler form uses the same
 * {@code nominalSize} keys.
 *
 * <p>Resolvers are the shared singletons from {@code ResolverConfig}; the fitting resolver is the
 * same instance the calculation endpoint uses, so the table shown is the table calculated against.
 */
@Service
public class SprinklerReferenceService {

	private final HazardPresetResolver hazardPresetResolver;
	private final CFactorResolver cFactorResolver;
	private final FittingEquivalentLengthResolver fittingResolver;
	private final SprinklerKFactorResolver kFactorResolver;

	public SprinklerReferenceService(
			HazardPresetResolver hazardPresetResolver,
			CFactorResolver cFactorResolver,
			FittingEquivalentLengthResolver fittingResolver,
			SprinklerKFactorResolver kFactorResolver) {
		this.hazardPresetResolver = hazardPresetResolver;
		this.cFactorResolver = cFactorResolver;
		this.fittingResolver = fittingResolver;
		this.kFactorResolver = kFactorResolver;
	}

	/** Raw rows of reference/sprinkler/hazard-presets.json. */
	public List<HazardPresetEntryDto> listHazardPresets() {
		return hazardPresetResolver.allEntries().stream().map(HazardPresetEntryDto::from).toList();
	}

	/** Raw rows of reference/sprinkler/hazen-williams-c.json. */
	public List<SprinklerCFactorEntryDto> listCFactors() {
		return cFactorResolver.allEntries().stream().map(SprinklerCFactorEntryDto::from).toList();
	}

	/** Raw rows of reference/sprinkler/fitting-equivalent-lengths.json. */
	public List<FittingEquivalentLengthEntryDto> listFittingEquivalentLengths() {
		return fittingResolver.allEntries().stream().map(FittingEquivalentLengthEntryDto::from).toList();
	}

	/** Raw rows of reference/sprinkler/sprinkler-k-factors.json. */
	public List<SprinklerKFactorEntryDto> listKFactors() {
		return kFactorResolver.allEntries().stream().map(SprinklerKFactorEntryDto::from).toList();
	}

}
