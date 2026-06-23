package se.sundsvall.caremanagement.eventlog.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the {@link ErrandEventInterceptor} on errand-scoped routes only, so the access log is built without
 * touching any resource or service.
 */
@Configuration
class EventLogWebConfiguration implements WebMvcConfigurer {

	private final ErrandEventInterceptor interceptor;

	EventLogWebConfiguration(final ErrandEventInterceptor interceptor) {
		this.interceptor = interceptor;
	}

	@Override
	public void addInterceptors(final InterceptorRegistry registry) {
		registry.addInterceptor(interceptor)
			.addPathPatterns("/{municipalityId}/{namespace}/errands/**");
	}
}
