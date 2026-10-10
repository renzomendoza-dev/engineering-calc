package com.renzoproject.calc_api.mechanical.sprinkler;

import com.renzoproject.calc.core.mechanical.sprinkler.SupplyCurvePoint;

/** One sampled point on the water supply curve, for plotting supply against demand. */
public record SupplyCurvePointDto(Double flowGpm, Double availablePressurePsi) {

	public static SupplyCurvePointDto from(SupplyCurvePoint point) {
		return new SupplyCurvePointDto(point.flowGpm(), point.availablePressurePsi());
	}

}
