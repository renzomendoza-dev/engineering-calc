package com.renzoproject.calc.core.smokecontrol.naturalventing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzoproject.calc.core.exception.CalculationException;
import java.io.IOException;
import java.io.InputStream;
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
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = JsonNaturalVentingReferenceResolver.class.getResourceAsStream(RESOURCE_PATH)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + RESOURCE_PATH);
			}
			return objectMapper.readValue(in, NaturalVentingReferenceFile.class);
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + RESOURCE_PATH, e);
		}
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
