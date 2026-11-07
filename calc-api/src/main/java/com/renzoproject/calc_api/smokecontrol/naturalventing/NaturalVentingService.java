package com.renzoproject.calc_api.smokecontrol.naturalventing;

import com.renzoproject.calc.core.smokecontrol.naturalventing.NaturalVentingCalculator;
import com.renzoproject.calc.core.smokecontrol.naturalventing.NaturalVentingReferenceResolver;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core, same pattern as the other smoke
 * control services. The calculator needs no reference data, so it is plainly constructed; the
 * reference tables are only served, as defaults for the form.
 */
@Service
public class NaturalVentingService {

	private final NaturalVentingCalculator calculator = new NaturalVentingCalculator();
	private final NaturalVentingReferenceResolver referenceResolver;

	public NaturalVentingService(NaturalVentingReferenceResolver referenceResolver) {
		this.referenceResolver = referenceResolver;
	}

	public NaturalVentingResponse calculate(NaturalVentingRequest request) {
		var result = calculator.calculate(NaturalVentingMapper.toCoreInput(request));
		return NaturalVentingMapper.toResponse(result);
	}

	public NaturalVentingReferenceResponse reference() {
		return new NaturalVentingReferenceResponse(
				referenceResolver.edition(),
				referenceResolver.note(),
				referenceResolver.allDischargeCoefficients().stream()
						.map(row -> new DischargeCoefficientEntryDto(row.key(), row.label(), row.dischargeCoefficient(), row.clause(), row.confidence()))
						.toList(),
				referenceResolver.allCommodities().stream()
						.map(row -> new CommodityHeatReleaseEntryDto(row.key(), row.label(), row.heatReleaseRateKwPerM2(), row.confidence()))
						.toList());
	}

}
