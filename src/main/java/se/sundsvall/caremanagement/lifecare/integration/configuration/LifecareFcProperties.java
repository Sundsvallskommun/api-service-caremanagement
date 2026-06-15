package se.sundsvall.caremanagement.lifecare.integration.configuration;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the Tieto/Lifecare FamilyCare (FC) integration. {@code url} is the FC base path (host +
 * {@code /WESE.FC.Api.FC}); {@code domain} and {@code key} are the FC tenant id and API key applied as query
 * parameters / {@code X-API-Key} header by {@link LifecareFcConfiguration}. The key is sensitive — keep it in a secret,
 * never in committed config, and out of request logging.
 */
@Validated
@ConfigurationProperties(prefix = "integration.lifecare-fc")
public record LifecareFcProperties(

	@NotBlank String url,

	@NotBlank String domain,

	@NotBlank String key,

	@DefaultValue("5") int connectTimeout,

	@DefaultValue("30") int readTimeout) {
}
