package com.trieu.tripplanner.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc metadata. Swagger UI at /swagger-ui.html, raw spec at /v3/api-docs (paths set in application.yml).
 * The bearer scheme adds the "Authorize" button: paste an access token once and every call sends it.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI tripPlannerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Trip Planner API")
                        .version("v1")
                        .description("Success responses are wrapped in ApiResponse, errors in ErrorResponse. "
                                + "Protected endpoints need 'Authorization: Bearer <accessToken>' from POST /api/v1/auth/login."))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

}
