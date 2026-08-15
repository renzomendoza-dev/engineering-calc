package com.renzoproject.calc.core.common;

/**
 * Public (not package-private, unlike most test fakes in this codebase) since
 * {@link AirPropertiesResolver} is a shared/common-package resource, reusable by any domain's
 * calculator tests -- {@code smokecontrol}'s {@code SmokeProductionCalculatorTest} is the first
 * consumer, but not expected to be the last.
 */
public class FakeAirPropertiesResolver implements AirPropertiesResolver {

	private final AirProperties properties;

	public FakeAirPropertiesResolver(AirProperties properties) {
		this.properties = properties;
	}

	@Override
	public AirProperties properties() {
		return properties;
	}

}
