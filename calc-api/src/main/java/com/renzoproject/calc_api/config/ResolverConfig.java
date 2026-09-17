package com.renzoproject.calc_api.config;

import com.renzoproject.calc.core.acoustics.AudibilityThresholdResolver;
import com.renzoproject.calc.core.acoustics.JsonAudibilityThresholdResolver;
import com.renzoproject.calc.core.common.AirPropertiesResolver;
import com.renzoproject.calc.core.common.JsonAirPropertiesResolver;
import com.renzoproject.calc.core.electrical.reference.ConductorPropertiesResolver;
import com.renzoproject.calc.core.mechanical.duct.AirDensityViscosityResolver;
import com.renzoproject.calc.core.mechanical.duct.AnalyticalAirDensityViscosityResolver;
import com.renzoproject.calc.core.mechanical.duct.DuctRoughnessResolver;
import com.renzoproject.calc.core.mechanical.duct.DuctVelocityLimitResolver;
import com.renzoproject.calc.core.mechanical.duct.JsonDuctRoughnessResolver;
import com.renzoproject.calc.core.mechanical.duct.JsonDuctVelocityLimitResolver;
import com.renzoproject.calc.core.mechanical.firepump.FirePumpCapacityResolver;
import com.renzoproject.calc.core.mechanical.firepump.FirePumpCurveRequirementsLoader;
import com.renzoproject.calc.core.mechanical.firepump.FirePumpMotorSizeResolver;
import com.renzoproject.calc.core.mechanical.firepump.JsonFirePumpCapacityResolver;
import com.renzoproject.calc.core.mechanical.firepump.JsonFirePumpCurveRequirementsLoader;
import com.renzoproject.calc.core.mechanical.firepump.JsonFirePumpMotorSizeResolver;
import com.renzoproject.calc.core.mechanical.pipe.FluidPropertiesResolver;
import com.renzoproject.calc.core.mechanical.pipe.JsonFluidPropertiesResolver;
import com.renzoproject.calc.core.mechanical.pipe.JsonPipeDimensionResolver;
import com.renzoproject.calc.core.mechanical.pump.JsonPumpMotorSizeResolver;
import com.renzoproject.calc.core.mechanical.pump.PumpMotorSizeResolver;
import com.renzoproject.calc.core.mechanical.storage.FireWaterDurationResolver;
import com.renzoproject.calc.core.mechanical.storage.FixtureUnitDemandResolver;
import com.renzoproject.calc.core.mechanical.storage.JsonFireWaterDurationResolver;
import com.renzoproject.calc.core.mechanical.storage.JsonFixtureUnitDemandResolver;
import com.renzoproject.calc.core.mechanical.storage.JsonPerCapitaConsumptionResolver;
import com.renzoproject.calc.core.mechanical.storage.PerCapitaConsumptionResolver;
import com.renzoproject.calc.core.smokecontrol.JsonSmokeControlDefaultsResolver;
import com.renzoproject.calc.core.smokecontrol.SmokeControlDefaultsResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The one place calc-api constructs calc-core's reference-data resolvers. Each is a singleton
 * bean, so every service that needs, say, pipe dimensions shares one parsed copy of the pipe JSON
 * instead of each service parsing its own at startup.
 *
 * <p>calc-core stays Spring-free: only this class knows about {@code @Bean}. Calculators are still
 * constructed plainly inside their services -- they hold no state of their own beyond the
 * resolvers passed in, so there's nothing to gain by making them beans too.
 *
 * <p>Beans are declared at the interface type the calculators accept, with one exception:
 * {@link JsonPipeDimensionResolver} is declared at its concrete type because it implements both
 * {@code PipeDimensionResolver} and {@code PipeRoughnessResolver}, and Spring matches injection
 * points against the declared return type -- declaring either interface alone would leave the
 * other unresolvable.
 *
 * <p><b>Every bean here is shared across concurrent requests, so it must be thread-safe.</b> All
 * current resolvers are either immutable after construction or (for
 * {@code JsonFluidPropertiesResolver}'s lazy cache) explicitly concurrent. A new resolver that
 * caches lazily needs the same treatment.
 */
@Configuration
public class ResolverConfig {

	// --- electrical ---

	@Bean
	public ConductorPropertiesResolver conductorPropertiesResolver() {
		return new ConductorPropertiesResolver();
	}

	// --- mechanical: pipe ---

	@Bean
	public JsonPipeDimensionResolver pipeDimensionResolver() {
		return new JsonPipeDimensionResolver();
	}

	@Bean
	public FluidPropertiesResolver fluidPropertiesResolver() {
		return new JsonFluidPropertiesResolver();
	}

	// --- mechanical: pump & fire pump ---

	@Bean
	public PumpMotorSizeResolver pumpMotorSizeResolver() {
		return new JsonPumpMotorSizeResolver();
	}

	@Bean
	public FirePumpMotorSizeResolver firePumpMotorSizeResolver() {
		return new JsonFirePumpMotorSizeResolver();
	}

	@Bean
	public FirePumpCapacityResolver firePumpCapacityResolver() {
		return new JsonFirePumpCapacityResolver();
	}

	@Bean
	public FirePumpCurveRequirementsLoader firePumpCurveRequirementsLoader() {
		return new JsonFirePumpCurveRequirementsLoader();
	}

	// --- mechanical: duct ---

	@Bean
	public AirDensityViscosityResolver airDensityViscosityResolver() {
		return new AnalyticalAirDensityViscosityResolver();
	}

	@Bean
	public DuctRoughnessResolver ductRoughnessResolver() {
		return new JsonDuctRoughnessResolver();
	}

	@Bean
	public DuctVelocityLimitResolver ductVelocityLimitResolver() {
		return new JsonDuctVelocityLimitResolver();
	}

	// --- mechanical: storage ---

	@Bean
	public PerCapitaConsumptionResolver perCapitaConsumptionResolver() {
		return new JsonPerCapitaConsumptionResolver();
	}

	@Bean
	public FixtureUnitDemandResolver fixtureUnitDemandResolver() {
		return new JsonFixtureUnitDemandResolver();
	}

	@Bean
	public FireWaterDurationResolver fireWaterDurationResolver() {
		return new JsonFireWaterDurationResolver();
	}

	// --- acoustics ---

	@Bean
	public AudibilityThresholdResolver audibilityThresholdResolver() {
		return new JsonAudibilityThresholdResolver();
	}

	// --- smoke control ---

	@Bean
	public AirPropertiesResolver airPropertiesResolver() {
		return new JsonAirPropertiesResolver();
	}

	@Bean
	public SmokeControlDefaultsResolver smokeControlDefaultsResolver() {
		return new JsonSmokeControlDefaultsResolver();
	}

}
