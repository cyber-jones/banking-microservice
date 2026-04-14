package com.banking.account.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SwaggerConfig — configures the OpenAPI / Swagger documentation.
 *
 * TEACHING POINT — Swagger / OpenAPI:
 *
 * SpringDoc auto-scans @RestController classes and generates API documentation.
 * Access it at: http://localhost:8081/swagger-ui.html
 *
 * This config adds:
 *  - API metadata (title, version, description, contact)
 *  - JWT authentication button in the Swagger UI
 *    so you can log in and test protected endpoints directly in the browser
 *
 * The SecurityScheme defines "bearerAuth" which matches the
 * @SecurityRequirement(name = "bearerAuth") on controllers.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI bankingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Banking Microservices API")
                        .description("Spring Boot Banking Application — Teaching Example\n\n" +
                                "## How to use\n" +
                                "1. Register at `POST /api/v1/auth/register`\n" +
                                "2. Login at `POST /api/v1/auth/login` to get a JWT token\n" +
                                "3. Click **Authorize** above and enter: `Bearer <your-token>`\n" +
                                "4. All protected endpoints are now accessible")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Banking Teaching App")
                                .email("teaching@banking.com"))
                        .license(new License().name("MIT")))
                // JWT Bearer Token support in Swagger UI
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token (without 'Bearer ' prefix)")));
    }
}
