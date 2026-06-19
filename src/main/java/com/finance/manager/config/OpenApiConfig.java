package com.finance.manager.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI tallyApi(@Value("${app.oauth.authorization-uri}") String authorizationUri,
                         @Value("${app.oauth.token-uri}") String tokenUri) {
        Scopes scopes = new Scopes()
                .addString("expenses.read", "Read owned expenses")
                .addString("expenses.write", "Create, update, and delete owned expenses")
                .addString("budget.read", "Read the owned budget")
                .addString("budget.write", "Create or update the owned budget");
        SecurityScheme oidc = new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .flows(new OAuthFlows().authorizationCode(new OAuthFlow()
                        .authorizationUrl(authorizationUri)
                        .tokenUrl(tokenUri)
                        .scopes(scopes)));
        return new OpenAPI()
                .info(new Info().title("Tally API").version("v1")
                        .description("OIDC-secured, user-isolated finance API backed by Hibernate"))
                .addSecurityItem(new SecurityRequirement().addList("oauth2"))
                .components(new Components().addSecuritySchemes("oauth2", oidc));
    }
}
