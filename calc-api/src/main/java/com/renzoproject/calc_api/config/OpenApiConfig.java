package com.renzoproject.calc_api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI engineeringCalcOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Engineering Calc API")
						.description("Stateless engineering calculators (voltage drop, PEC reference data) — v1, no persistence, no auth.")
						.version("0.0.1"));
	}

}
