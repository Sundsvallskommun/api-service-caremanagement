package se.sundsvall.caremanagement.lifecare.integration.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

/**
 * Builds the {@link se.sundsvall.caremanagement.lifecare.integration.LifecareFcClient} customizer. FC authenticates
 * with
 * a {@code domain} + {@code key}, both required as query parameters; the spec also accepts the key as an
 * {@code X-API-Key} header, so we send both. The header is harmless where ignored and lets us drop the query-string key
 * once Tieto confirms header auth fleet-wide.
 */
@Import(FeignConfiguration.class)
@EnableConfigurationProperties(LifecareFcProperties.class)
public class LifecareFcConfiguration {

	public static final String CLIENT_ID = "lifecare-fc";

	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(final LifecareFcProperties properties) {
		return FeignMultiCustomizer.create()
			.withErrorDecoder(new ProblemErrorDecoder(CLIENT_ID))
			.withRequestInterceptor(template -> {
				template.query("domain", properties.domain());
				template.query("key", properties.key());
				template.header("X-API-Key", properties.key());
			})
			.withRequestTimeoutsInSeconds(properties.connectTimeout(), properties.readTimeout())
			.composeCustomizersToOne();
	}
}
