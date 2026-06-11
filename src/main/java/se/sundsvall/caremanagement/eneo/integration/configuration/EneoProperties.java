package se.sundsvall.caremanagement.eneo.integration.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the Eneo (Sundsvall LLM platform) integration. {@code url} + {@code apiKey} are flat properties so
 * they bind cleanly from environment variables; the OAuth2 client-credentials registration (api-gateway auth) is built
 * programmatically in {@link EneoConfiguration}. Type-agnostic — the assistant to ask is supplied per call, so no
 * assistant ids live here.
 */
@Validated
@ConfigurationProperties(prefix = "integration.eneo")
public record EneoProperties(

	@NotBlank String url,

	@NotBlank String apiKey,

	@Valid @NotNull Oauth2 oauth2,

	@DefaultValue("5") int connectTimeoutInSeconds,

	@DefaultValue("30") int readTimeoutInSeconds) {

	public record Oauth2(
		@NotBlank String tokenUrl,
		@NotBlank String clientId,
		@NotBlank String clientSecret,
		@DefaultValue("client_credentials") String authorizationGrantType) {
	}
}
