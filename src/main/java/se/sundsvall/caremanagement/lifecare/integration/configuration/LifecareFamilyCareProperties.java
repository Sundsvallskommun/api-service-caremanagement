package se.sundsvall.caremanagement.lifecare.integration.configuration;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import static java.util.Optional.ofNullable;

/**
 * Configuration for the Tieto/Lifecare FamilyCare integration. {@code url} is the FamilyCare base path (host + {@code
 * /WESE.FC.Api.FC}); {@code domain} and {@code key} are the FamilyCare tenant id and API key applied as query
 * parameters / {@code X-API-Key} header by {@link LifecareFamilyCareConfiguration}. The keys are sensitive — keep them
 * in a secret, never in committed config, and out of request logging.
 *
 * <p>
 * FamilyCare licences its APIs per consumer, and the {@code Users/*} directory is a different licence from the
 * person-based case APIs (in Sundsvall: "Användarinformation IFO" versus "API IFO Personbaserade"). {@code userKey}
 * carries that second licence key and is optional — where one consumer covers both surfaces, and in mocked
 * environments, leave it unset and {@code key} is used for every call.
 */
@Validated
@ConfigurationProperties(prefix = "integration.lifecare-familycare")
public record LifecareFamilyCareProperties(

	@NotBlank String url,

	@NotBlank String domain,

	@NotBlank String key,

	String userKey,

	@DefaultValue("5") int connectTimeout,

	@DefaultValue("30") int readTimeout) {

	/**
	 * The key to authenticate the {@code Users/*} endpoints with: the separate user-directory licence key when one is
	 * configured, otherwise the main {@link #key()}.
	 *
	 * @return the user-directory key, never blank
	 */
	public String userKeyOrDefault() {
		return ofNullable(userKey).filter(StringUtils::hasText).orElse(key);
	}
}
