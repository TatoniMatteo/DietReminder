package it.matato.dietreminder.backend.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun dietReminderOpenApi(): OpenAPI = OpenAPI()
        .info(Info().title("Diet Reminder API").version("1")
            .description("Accounts, profiles and versioned cloud workspaces. Register or sign in, then paste the session token into Authorize to try authenticated endpoints."))
        .components(Components().addSecuritySchemes("cookieAuth", SecurityScheme().type(SecurityScheme.Type.APIKEY).`in`(SecurityScheme.In.COOKIE).name("dietreminder_session")
            .description("Web session cookie. Unsafe cookie-authenticated requests also require X-CSRF-Token from the web session response."))
            .addSecuritySchemes("bearerAuth", SecurityScheme()
            .type(SecurityScheme.Type.HTTP).scheme("bearer")
            .description("Session token returned by /api/auth/login or /api/auth/register. Enter the token without the Bearer prefix.")))
        .addSecurityItem(SecurityRequirement().addList("bearerAuth"))
        .addSecurityItem(SecurityRequirement().addList("cookieAuth"))
}
