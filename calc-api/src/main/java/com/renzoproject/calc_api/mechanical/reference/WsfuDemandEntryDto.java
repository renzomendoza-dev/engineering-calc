package com.renzoproject.calc_api.mechanical.reference;

import com.renzoproject.calc.core.mechanical.storage.WsfuDemandRow;

/**
 * HTTP response representation of one row of reference/storage/wsfu-demand.json, for
 * populating a frontend reference table — display only, not used in any calculation path.
 * gpmFlushValves is nullable, same as the underlying row (no published data below WSFU=5).
 */
public record WsfuDemandEntryDto(double wsfu, double gpmFlushTanks, Double gpmFlushValves) {

	public static WsfuDemandEntryDto from(WsfuDemandRow row) {
		return new WsfuDemandEntryDto(row.wsfu(), row.gpmFlushTanks(), row.gpmFlushValves());
	}

}
