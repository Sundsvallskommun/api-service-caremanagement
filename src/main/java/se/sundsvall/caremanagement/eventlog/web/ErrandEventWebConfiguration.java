package se.sundsvall.caremanagement.eventlog.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Wires the {@link ErrandEventInterceptor} on the errand-scoped routes — it records the who/what/when access log.
 *
 * <p>
 * The {@code municipalityId} path variable is constrained to {@code [0-9]+} so the pattern matches only real
 * municipality-scoped paths ({@code /2281/...}). Without it, {@code /{municipalityId}/**} would greedily capture any
 * first segment — {@code /api-docs}, {@code /swagger-ui/...}. (Actuator is served by a separate handler mapping and is
 * unaffected by MVC interceptors regardless.)
 */
@Configuration
class ErrandEventWebConfiguration implements WebMvcConfigurer {

	static final String ERRAND_PATTERN = "/{municipalityId:[0-9]+}/{namespace}/errands/**";

	private final ErrandEventInterceptor errandEventInterceptor;

	ErrandEventWebConfiguration(final ErrandEventInterceptor errandEventInterceptor) {
		this.errandEventInterceptor = errandEventInterceptor;
	}

	@Override
	public void addInterceptors(final InterceptorRegistry registry) {
		registry.addInterceptor(errandEventInterceptor)
			.addPathPatterns(ERRAND_PATTERN);
	}
}
