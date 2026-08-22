package com.renzoproject.calc.core.mechanical.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzoproject.calc.core.exception.CalculationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;

/**
 * Loads {@code reference/storage/wsfu-demand.json} from the classpath once. Mirrors
 * {@code JsonPipeDimensionResolver}'s loading approach: fresh {@code ObjectMapper}, hardcoded
 * resource path constant, {@code CalculationException} on a missing/unreadable resource, parsed
 * eagerly in the constructor and cached rather than re-read per call.
 */
public class JsonFixtureUnitDemandResolver implements FixtureUnitDemandResolver {

	private static final String RESOURCE_PATH = "/reference/storage/wsfu-demand.json";
	private static final double MAX_WSFU = 10000.0;
	private static final double MIN_FLUSH_VALVE_WSFU = 5.0;

	private final List<WsfuDemandRow> demandTable;

	public JsonFixtureUnitDemandResolver() {
		WsfuDemandFile file = load();
		this.demandTable = file.demandTable().stream()
				.sorted(Comparator.comparingDouble(WsfuDemandRow::wsfu))
				.toList();
	}

	private static WsfuDemandFile load() {
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = JsonFixtureUnitDemandResolver.class.getResourceAsStream(RESOURCE_PATH)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + RESOURCE_PATH);
			}
			return objectMapper.readValue(in, WsfuDemandFile.class);
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + RESOURCE_PATH, e);
		}
	}

	@Override
	public double resolveGpm(double totalWsfu, SystemType systemType) {
		if (systemType == null) {
			throw new CalculationException("systemType is required");
		}
		if (totalWsfu > MAX_WSFU) {
			throw new CalculationException("totalWsfu " + totalWsfu + " exceeds the table's upper bound of "
					+ MAX_WSFU + " WSFU -- cannot extrapolate");
		}
		if (systemType == SystemType.FLUSH_VALVE && totalWsfu < MIN_FLUSH_VALVE_WSFU) {
			throw new CalculationException("No published flush-valve demand data below WSFU=" + MIN_FLUSH_VALVE_WSFU
					+ " (gpmFlushValves is null in the source table there) -- got totalWsfu=" + totalWsfu);
		}

		List<WsfuDemandRow> usableRows = demandTable.stream()
				.filter(row -> gpmFor(row, systemType) != null)
				.toList();

		WsfuDemandRow lower = null;
		WsfuDemandRow upper = null;
		for (WsfuDemandRow row : usableRows) {
			if (row.wsfu() <= totalWsfu) {
				lower = row;
			}
			if (row.wsfu() >= totalWsfu) {
				upper = row;
				break;
			}
		}

		if (lower == null) {
			double smallest = usableRows.get(0).wsfu();
			throw new CalculationException("totalWsfu " + totalWsfu + " is below the table's lowest known value ("
					+ smallest + " WSFU) for " + systemType + " -- cannot extrapolate");
		}
		if (upper == null) {
			throw new CalculationException("totalWsfu " + totalWsfu + " has no upper bracket in the table for " + systemType);
		}
		if (lower.wsfu() == upper.wsfu()) {
			return gpmFor(lower, systemType);
		}

		double fraction = (totalWsfu - lower.wsfu()) / (upper.wsfu() - lower.wsfu());
		double lowerGpm = gpmFor(lower, systemType);
		double upperGpm = gpmFor(upper, systemType);
		return lowerGpm + fraction * (upperGpm - lowerGpm);
	}

	/**
	 * Both branches must stay boxed {@code Double} -- a ternary mixing a primitive
	 * {@code double} branch ({@code gpmFlushTanks()}) with a boxed {@code Double} branch
	 * ({@code gpmFlushValves()}) triggers Java's binary numeric promotion (JLS 15.25), which
	 * unboxes the boxed branch unconditionally to make both branches primitive -- throwing an
	 * NPE on a null {@code gpmFlushValves()} even when the FLUSH_TANK branch is the one actually
	 * selected. {@code Double.valueOf(...)} keeps both branches reference-typed so only the
	 * selected branch's value is used, null included.
	 */
	private static Double gpmFor(WsfuDemandRow row, SystemType systemType) {
		return systemType == SystemType.FLUSH_TANK ? Double.valueOf(row.gpmFlushTanks()) : row.gpmFlushValves();
	}

	@Override
	public List<WsfuDemandRow> allEntries() {
		return demandTable;
	}

}
