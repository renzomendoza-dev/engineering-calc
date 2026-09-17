package com.renzoproject.calc_api.config;

import com.renzoproject.calc.core.mechanical.pipe.FluidPropertiesResolver;
import com.renzoproject.calc.core.mechanical.pipe.PipeDimensionResolver;
import com.renzoproject.calc.core.mechanical.pipe.PipeRoughnessResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Pins the point of {@link ResolverConfig}: each reference dataset is loaded once and shared,
 * not re-parsed per service.
 */
@SpringBootTest
class ResolverConfigTest {

	@Autowired
	private ApplicationContext context;

	@Test
	void fluidPropertiesResolver_isASingleSharedInstance() {
		// Used by pipe pressure loss, pump TDH, and expansion tank. Before ResolverConfig each of
		// those services built its own, each with its own lazy cache.
		assertEquals(1, context.getBeansOfType(FluidPropertiesResolver.class).size());
	}

	@Test
	void pipeDimensionAndRoughness_resolveToTheSameInstance() {
		// JsonPipeDimensionResolver implements both interfaces; one parse of the pipe JSON should
		// serve both injection points across every pipe/pump/reference service.
		assertEquals(1, context.getBeansOfType(PipeDimensionResolver.class).size());
		assertSame(context.getBean(PipeDimensionResolver.class), context.getBean(PipeRoughnessResolver.class));
	}

	@Test
	void bothAirPropertiesResolvers_areRegisteredWithoutAmbiguity() {
		// Two distinct interfaces share this simple name; each must resolve to exactly one bean.
		assertEquals(1, context.getBeansOfType(com.renzoproject.calc.core.common.AirPropertiesResolver.class).size());
		assertEquals(1, context.getBeansOfType(com.renzoproject.calc.core.mechanical.duct.AirPropertiesResolver.class).size());
	}

}
