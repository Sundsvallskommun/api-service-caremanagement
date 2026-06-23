package se.sundsvall.caremanagement.eventlog.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Wires the event-log interceptors:
 * <ul>
 * <li>{@link RequireIdentifierInterceptor} on the whole {@code /{municipalityId}/**} business surface — rejects
 * requests
 * without an {@code X-Sent-By} identity, so every logged action is attributable. Registered first so anonymous requests
 * are turned away before the access log engages.</li>
 * <li>{@link ErrandEventInterceptor} on errand-scoped routes — records the who/what/when access log.</li>
 * </ul>
 */
@Configuration
class EventLogWebConfiguration implements WebMvcConfigurer {

	private final RequireIdentifierInterceptor requireIdentifierInterceptor;
	private final ErrandEventInterceptor errandEventInterceptor;

	EventLogWebConfiguration(final RequireIdentifierInterceptor requireIdentifierInterceptor, final ErrandEventInterceptor errandEventInterceptor) {
		this.requireIdentifierInterceptor = requireIdentifierInterceptor;
		this.errandEventInterceptor = errandEventInterceptor;
	}

	@Override
	public void addInterceptors(final InterceptorRegistry registry) {
		registry.addInterceptor(requireIdentifierInterceptor)
			.addPathPatterns("/{municipalityId}/**");
		registry.addInterceptor(errandEventInterceptor)
			.addPathPatterns("/{municipalityId}/{namespace}/errands/**");
	}
}
