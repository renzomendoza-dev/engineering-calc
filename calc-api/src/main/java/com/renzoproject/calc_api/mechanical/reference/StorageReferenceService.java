package com.renzoproject.calc_api.mechanical.reference;

import com.renzoproject.calc.core.mechanical.storage.FireWaterDurationResolver;
import com.renzoproject.calc.core.mechanical.storage.FixtureUnitDemandResolver;
import com.renzoproject.calc.core.mechanical.storage.PerCapitaConsumptionResolver;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Exposes calc-core's water storage reference tables (lpcd consumption, WSFU demand,
 * fire water duration) for display purposes -- so a user can verify a Domestic/Fire Water
 * Storage recommendation by hand, same role as ConductorReferenceService's ampacity/derating
 * table endpoints for Wire Sizing.
 *
 * <p>Resolvers are the shared singletons from {@code ResolverConfig} -- the same instances the
 * storage calculators use, so the tables displayed here are exactly the data the calculations
 * run against.
 */
@Service
public class StorageReferenceService {

	private final PerCapitaConsumptionResolver perCapitaConsumptionResolver;
	private final FixtureUnitDemandResolver fixtureUnitDemandResolver;
	private final FireWaterDurationResolver fireWaterDurationResolver;

	public StorageReferenceService(
			PerCapitaConsumptionResolver perCapitaConsumptionResolver,
			FixtureUnitDemandResolver fixtureUnitDemandResolver,
			FireWaterDurationResolver fireWaterDurationResolver) {
		this.perCapitaConsumptionResolver = perCapitaConsumptionResolver;
		this.fixtureUnitDemandResolver = fixtureUnitDemandResolver;
		this.fireWaterDurationResolver = fireWaterDurationResolver;
	}

	/** Raw rows of reference/storage/lpcd-consumption.json, for display purposes. */
	public List<OccupancyTypeEntryDto> listLpcdConsumptionTable() {
		return perCapitaConsumptionResolver.allEntries().stream()
				.map(OccupancyTypeEntryDto::from)
				.toList();
	}

	/** Raw rows of reference/storage/wsfu-demand.json, for display purposes. */
	public List<WsfuDemandEntryDto> listWsfuDemandTable() {
		return fixtureUnitDemandResolver.allEntries().stream()
				.map(WsfuDemandEntryDto::from)
				.toList();
	}

	/** Raw rows of reference/storage/fire-water-duration.json, for display purposes. */
	public List<FireWaterDurationEntryDto> listFireWaterDurationTable() {
		return fireWaterDurationResolver.allEntries().stream()
				.map(FireWaterDurationEntryDto::from)
				.toList();
	}

}
