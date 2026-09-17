package com.renzoproject.calc_api.electrical.voltagedrop;

import com.renzoproject.calc.core.electrical.reference.ConductorProperties;
import com.renzoproject.calc.core.electrical.reference.ConductorPropertiesResolver;
import com.renzoproject.calc.core.electrical.reference.ConductorSize;
import com.renzoproject.calc.core.electrical.voltagedrop.VoltageDropCalculator;
import com.renzoproject.calc.core.exception.CalculationException;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core: resolves conductor R/X (either from
 * PEC reference tables or the request's custom values), maps the request DTO to
 * {@code VoltageDropInput}, runs the calculator, maps the result back to a response DTO.
 *
 * <p>{@link ConductorPropertiesResolver} is the shared singleton from {@code ResolverConfig}
 * (the same instance {@code ConductorReferenceService} uses), since it holds parsed PEC tables.
 * {@link VoltageDropCalculator} has no dependencies and no state, so it's plainly constructed.
 */
@Service
public class VoltageDropService {

	private final VoltageDropCalculator calculator = new VoltageDropCalculator();
	private final ConductorPropertiesResolver resolver;

	public VoltageDropService(ConductorPropertiesResolver resolver) {
		this.resolver = resolver;
	}

	public VoltageDropResponse calculate(VoltageDropRequest request) {
		double resistanceOhmsPerMeter;
		double reactanceOhmsPerMeter;

		if (request.isUseCustomImpedance()) {
			resistanceOhmsPerMeter = request.getCustomResistanceOhmsPerMeter();
			reactanceOhmsPerMeter = request.getCustomReactanceOhmsPerMeter();
		} else {
			ConductorSize size = findConductorSize(request.getConductorSizeLabel());
			ConductorProperties properties = resolver.resolve(
					request.getCircuitType(), size, request.getConductorMaterial(), request.getConduitMaterial());
			resistanceOhmsPerMeter = properties.resistanceOhmsPerMeter();
			reactanceOhmsPerMeter = properties.reactanceOhmsPerMeter();
		}

		var input = VoltageDropMapper.toInput(request, resistanceOhmsPerMeter, reactanceOhmsPerMeter);
		var result = calculator.calculate(input);
		return VoltageDropMapper.toResponse(result);
	}

	private ConductorSize findConductorSize(String label) {
		return resolver.allSizes().stream()
				.filter(size -> size.label().equals(label))
				.findFirst()
				.orElseThrow(() -> new CalculationException("Unknown conductor size: " + label));
	}

}
