package com.renzoproject.calc_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows the local frontend (engineering-calc-web, {@code http://localhost:3000}) to call
 * this API's endpoints. v1 has no auth/cookies, so credentials are not enabled.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins("http://localhost:3000")
				.allowedMethods("GET", "POST");
	}

}
