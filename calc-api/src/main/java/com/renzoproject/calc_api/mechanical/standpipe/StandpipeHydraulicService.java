package com.renzoproject.calc_api.mechanical.standpipe;

import com.renzoproject.calc.core.mechanical.pipe.PipeDimensionResolver;
import com.renzoproject.calc.core.mechanical.standpipe.StandpipeHydraulicCalculator;
import com.renzoproject.calc.core.mechanical.standpipe.StandpipeReferenceResolver;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core, the same pattern as the sprinkler
 * service: shared resolvers injected, calculator plainly constructed. Stateless, so a page can
 * call it on each debounced input change.
 */
@Service
public class StandpipeHydraulicService {

	private final StandpipeHydraulicCalculator calculator;
	private final StandpipeReferenceResolver referenceResolver;

	public StandpipeHydraulicService(PipeDimensionResolver dimensionResolver, StandpipeReferenceResolver referenceResolver) {
		this.calculator = new StandpipeHydraulicCalculator(dimensionResolver, referenceResolver);
		this.referenceResolver = referenceResolver;
	}

	public StandpipeHydraulicResponse calculate(StandpipeHydraulicRequest request) {
		var input = StandpipeHydraulicMapper.toCoreInput(request);
		return StandpipeHydraulicMapper.toResponse(calculator.calculate(input));
	}

	public StandpipeReferenceResponse reference() {
		return new StandpipeReferenceResponse(
				referenceResolver.edition(),
				referenceResolver.note(),
				referenceResolver.allFittings().stream()
						.map(row -> new StandpipeFittingEntryDto(row.fittingType(), row.label(), row.confidence(), row.equivalentLengthFtByNominalSize()))
						.toList(),
				referenceResolver.allCFactors().stream()
						.map(row -> new StandpipeCFactorEntryDto(row.pipeType(), row.label(), row.cFactor(), row.confidence()))
						.toList());
	}

}
