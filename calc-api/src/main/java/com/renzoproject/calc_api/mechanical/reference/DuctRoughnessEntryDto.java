package com.renzoproject.calc_api.mechanical.reference;

import com.renzoproject.calc.core.mechanical.duct.DuctRoughnessRow;

/**
 * HTTP response representation of one row of reference/duct/duct-roughness.json, for
 * populating a frontend material dropdown and reference table — display only, not used in
 * any calculation path. sourceNote carries the per-entry provenance/caveat text (e.g. the
 * "tentatively medium rough, no numeric data available" flag on FIBERGLASS_DUCT_RIGID, or
 * CONCRETE_DUCT's unusually wide source range) even though the file's overall confidence is
 * "verified" — a frontend caveat badge should key off this field for those specific entries,
 * not the file-level confidence.
 */
public record DuctRoughnessEntryDto(String material, String label, double absoluteRoughnessMm, String sourceNote) {

	public static DuctRoughnessEntryDto from(DuctRoughnessRow row) {
		return new DuctRoughnessEntryDto(row.material(), row.label(), row.absoluteRoughnessMm(), row.sourceNote());
	}

}
