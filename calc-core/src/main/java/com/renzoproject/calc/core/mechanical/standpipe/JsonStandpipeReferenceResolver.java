package com.renzoproject.calc.core.mechanical.standpipe;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.reference.ReferenceJson;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Loads {@code reference/standpipe/standpipe-reference.json} from the classpath once, the same way
 * as the sprinkler package's JSON resolvers.
 */
public class JsonStandpipeReferenceResolver implements StandpipeReferenceResolver {

	private static final String RESOURCE_PATH = "/reference/standpipe/standpipe-reference.json";

	private final StandpipeReferenceFile file;
	private final Map<String, StandpipeFittingRow> fittingsByType;

	public JsonStandpipeReferenceResolver() {
		this.file = load();
		this.fittingsByType = file.fittings().stream()
				.collect(Collectors.toMap(row -> row.fittingType().toUpperCase(), Function.identity()));
	}

	private static StandpipeReferenceFile load() {
		return ReferenceJson.read(RESOURCE_PATH, StandpipeReferenceFile.class);
	}

	@Override
	public StandpipeFittingRow resolveFitting(String fittingType) {
		if (fittingType == null || fittingType.isBlank()) {
			throw new CalculationException("fittingType is required");
		}
		StandpipeFittingRow row = fittingsByType.get(fittingType.toUpperCase());
		if (row == null) {
			throw new CalculationException("Unknown fitting type: " + fittingType
					+ " (known fitting types: " + fittingsByType.keySet() + ")");
		}
		return row;
	}

	@Override
	public double cFactorBasis() {
		return file.cFactorBasis();
	}

	@Override
	public List<StandpipeFittingRow> allFittings() {
		return file.fittings();
	}

	@Override
	public List<StandpipeCFactorRow> allCFactors() {
		return file.cFactors();
	}

	@Override
	public List<StandpipeHoseNozzleRow> allHoseNozzleCombinations() {
		return file.hoseNozzleCombinations();
	}

	@Override
	public String hoseNozzleNote() {
		return file.hoseNozzleNote();
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
