package se.sundsvall.caremanagement.eventlog.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Wires the event-log interceptors:
 * <ul>
 * <li>{@link RequireIdentifierInterceptor} on the whole municipality-scoped business surface — rejects requests without
 * an {@code X-Sent-By} identity, so every logged action is attributable. Registered first so anonymous requests are
 * turned away before the access log engages.</li>
 * <li>{@link ErrandEventInterceptor} on errand-scoped routes — records the who/what/when access log.</li>
 * </ul>
 *
 * <p>
 * The {@code municipalityId} path variable is constrained to {@code [0-9]+} so the patterns match only real
 * municipality-scoped paths ({@code /2281/...}). Without it, {@code /{municipalityId}/**} would greedily capture any
 * first segment — {@code /api-docs}, {@code /swagger-ui/...} — and 400 the API docs. (Actuator is served by a separate
 * handler mapping and is unaffected by MVC interceptors regardless.)
 */
@Configuration
class EventLogWebConfiguration implements WebMvcConfigurer {

	static final String BUSINESS_API_PATTERN = "/{municipalityId:[0-9]+}/**";
	static final String ERRAND_PATTERN = "/{municipalityId:[0-9]+}/{namespace}/errands/**";

	private final RequireIdentifierInterceptor requireIdentifierInterceptor;
	private final ErrandEventInterceptor errandEventInterceptor;

	EventLogWebConfiguration(final RequireIdentifierInterceptor requireIdentifierInterceptor, final ErrandEventInterceptor errandEventInterceptor) {
		this.requireIdentifierInterceptor = requireIdentifierInterceptor;
		this.errandEventInterceptor = errandEventInterceptor;
	}

	@Override
	public void addInterceptors(final InterceptorRegistry registry) {
		registry.addInterceptor(requireIdentifierInterceptor)
			.addPathPatterns(BUSINESS_API_PATTERN);
		registry.addInterceptor(errandEventInterceptor)
			.addPathPatterns(ERRAND_PATTERN);
	}
}
