package com.example.crm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
@EnableMongoAuditing
public class AppConfig {

	// Swagger UI With An "Authorize" Button For The JWT -> http://localhost:8080/swagger-ui.html
	@Bean
	public OpenAPI chatAppOpenApi() {
		return new OpenAPI()
				.info(new Info().title("Nexus Chat API").version("v1")
						.description("WhatsApp + Discord style chat backend (Spring Boot, MongoDB, Redis, STOMP)"))
				.addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
				.components(new Components().addSecuritySchemes("bearerAuth",
						new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
	}
}
