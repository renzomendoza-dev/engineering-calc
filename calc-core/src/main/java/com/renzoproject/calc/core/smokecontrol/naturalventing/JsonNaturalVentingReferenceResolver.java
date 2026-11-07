package com.renzoproject.calc.core.smokecontrol.naturalventing;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.reference.ReferenceJson;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads {@code reference/smoke-control/natural-venting-reference.json} from the classpath once,
 * the same way as the other JSON resolvers. Keys must be unique within each table.
 */
public class JsonNaturalVentingReferenceResolver implements NaturalVentingReferenceResolver {

	private static final String RESOURCE_PATH = "/reference/smoke-control/natural-venting-reference.json";

	private final NaturalVentingReferenceFile file;

	public JsonNaturalVentingReferenceResolver() {
		this.file = load();
		requireUniqueKeys("discharge coefficient", file.dischargeCoefficients().stream().map(VentDischargeCoefficientRow::key).toList());
		requireUniqueKeys("commodity", file.commodities().stream().map(CommodityHeatReleaseRow::key).toList());
	}

	private static NaturalVentingReferenceFile load() {
		return ReferenceJson.read(RESOURCE_PATH, NaturalVentingReferenceFile.class);
	}

	private static void requireUniqueKeys(String table, List<String> keys) {
		Set<String> seen = new HashSet<>();
		for (String key : keys) {
			if (!seen.add(key)) {
				throw new CalculationException("Duplicate " + table + " key in " + RESOURCE_PATH + ": " + key);
			}
		}
	}

	@Override
	public List<VentDischargeCoefficientRow> allDischargeCoefficients() {
		return file.dischargeCoefficients();
	}

	@Override
	public List<CommodityHeatReleaseRow> allCommodities() {
		return file.commodities();
	}

	@Override
	public String edition() {
		return file.edition();
	}

	@Override
	public String note() {
		return file.note();
	}

}
