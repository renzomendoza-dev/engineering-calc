package com.renzoproject.calc_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows configured frontend origins (engineering-calc-web, local and deployed) to call this
 * API's endpoints. v1 has no auth/cookies, so credentials are not enabled.
 *
 * <p>Origins come from the {@code cors.allowed-origins} property (comma-separated), which falls
 * back to local dev ({@code http://localhost:3000}) plus the deployed frontend
 * ({@code https://eng-tools.renzomendoza.com}) when unset. Override via the
 * {@code CORS_ALLOWED_ORIGINS} environment variable -- Spring Boot's relaxed binding maps it to
 * the property above -- so origins can change per environment (e.g. to add the docker-compose
 * stack's locally-published frontend port) without a rebuild.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

	private final String[] allowedOrigins;

	public CorsConfig(
			@Value("${cors.allowed-origins:http://localhost:3000,https://eng-tools.renzomendoza.com}") String[] allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins(allowedOrigins)
				.allowedMethods("GET", "POST");
	}

}
