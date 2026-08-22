package com.renzoproject.calc_api.mechanical.reference;

import com.renzoproject.calc.core.mechanical.storage.FireWaterDurationResolver;
import com.renzoproject.calc.core.mechanical.storage.FixtureUnitDemandResolver;
import com.renzoproject.calc.core.mechanical.storage.JsonFireWaterDurationResolver;
import com.renzoproject.calc.core.mechanical.storage.JsonFixtureUnitDemandResolver;
import com.renzoproject.calc.core.mechanical.storage.JsonPerCapitaConsumptionResolver;
import com.renzoproject.calc.core.mechanical.storage.PerCapitaConsumptionResolver;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Exposes calc-core's water storage reference tables (lpcd consumption, WSFU demand,
 * fire water duration) for display purposes -- so a user can verify a Domestic/Fire Water
 * Storage recommendation by hand, same role as ConductorReferenceService's ampacity/derating
 * table endpoints for Wire Sizing.
 *
 * <p>Resolvers are plainly instantiated, same pattern as PipeReferenceService/VoltageDropService
 * -- see those classes' Javadoc for why.
 */
@Service
public class StorageReferenceService {

	private final PerCapitaConsumptionResolver perCapitaConsumptionResolver = new JsonPerCapitaConsumptionResolver();
	private final FixtureUnitDemandResolver fixtureUnitDemandResolver = new JsonFixtureUnitDemandResolver();
	private final FireWaterDurationResolver fireWaterDurationResolver = new JsonFireWaterDurationResolver();

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
