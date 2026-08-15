package com.renzoproject.calc.core.acoustics;

class FakeAudibilityThresholdResolver implements AudibilityThresholdResolver {

	private final NotificationModeThreshold publicMode;
	private final NotificationModeThreshold privateMode;
	private final SleepingAreaThreshold sleepingArea;
	private final SystemWideLimits systemWideLimits;

	FakeAudibilityThresholdResolver(
			NotificationModeThreshold publicMode,
			NotificationModeThreshold privateMode,
			SleepingAreaThreshold sleepingArea,
			SystemWideLimits systemWideLimits) {
		this.publicMode = publicMode;
		this.privateMode = privateMode;
		this.sleepingArea = sleepingArea;
		this.systemWideLimits = systemWideLimits;
	}

	@Override
	public NotificationModeThreshold publicMode() {
		return publicMode;
	}

	@Override
	public NotificationModeThreshold privateMode() {
		return privateMode;
	}

	@Override
	public SleepingAreaThreshold sleepingArea() {
		return sleepingArea;
	}

	@Override
	public SystemWideLimits systemWideLimits() {
		return systemWideLimits;
	}

}
