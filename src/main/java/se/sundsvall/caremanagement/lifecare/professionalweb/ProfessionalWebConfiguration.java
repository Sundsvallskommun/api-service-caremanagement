package se.sundsvall.caremanagement.lifecare.professionalweb;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.sundsvall.dept44.security.Truststore;

/**
 * Registers the ProfessionalWeb properties, and the HTTP client every ProfessionalWeb call goes through. The other
 * beans are components in this package.
 */
@Configuration
@EnableConfigurationProperties(ProfessionalWebProperties.class)
class ProfessionalWebConfiguration {

	@Bean
	HttpClient professionalWebHttpClient(final ProfessionalWebProperties properties, final Truststore truststore) {
		return HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NEVER)
			// Lifecare runs on IIS, which the fleet has pinned to HTTP/1.1 elsewhere for good reason.
			.version(HttpClient.Version.HTTP_1_1)
			.connectTimeout(Duration.ofSeconds(properties.connectTimeout()))
			.sslContext(truststore.getSSLContext())
			.build();
	}
}
