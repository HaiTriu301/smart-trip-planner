package com.trieu.tripplanner.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc metadata. Swagger UI at /swagger-ui.html, raw spec at /v3/api-docs (paths set in application.yml).
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI tripPlannerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Trip Planner API")
                        .version("v1")
                        .description("Success responses are wrapped in ApiResponse, errors in ErrorResponse."));
    }

}
