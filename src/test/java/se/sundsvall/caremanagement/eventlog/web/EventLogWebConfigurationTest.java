package se.sundsvall.caremanagement.eventlog.web;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.PathContainer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.util.pattern.PathPatternParser;
import se.sundsvall.caremanagement.eventlog.service.ErrandEventService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EventLogWebConfigurationTest {

	@Test
	void registersBothInterceptors() {
		final var configuration = new EventLogWebConfiguration(
			new RequireIdentifierInterceptor(true),
			new ErrandEventInterceptor(mock(ErrandEventService.class)));
		final var registry = new InterceptorRegistry();

		configuration.addInterceptors(registry);

		final List<?> interceptors = ReflectionTestUtils.invokeMethod(registry, "getInterceptors");
		assertThat(interceptors).hasSize(2);
	}

	@Test
	void businessApiPatternMatchesOnlyNumericMunicipalityPaths() {
		final var pattern = new PathPatternParser().parse(EventLogWebConfiguration.BUSINESS_API_PATTERN);

		// real municipality-scoped business paths are gated
		assertThat(pattern.matches(PathContainer.parsePath("/2281/FINANCIAL_ASSISTANCE/errands/abc"))).isTrue();
		assertThat(pattern.matches(PathContainer.parsePath("/2281/FINANCIAL_ASSISTANCE/metadata"))).isTrue();

		// infra / non-numeric first segments must NOT be gated (so api docs + swagger keep working)
		assertThat(pattern.matches(PathContainer.parsePath("/api-docs"))).isFalse();
		assertThat(pattern.matches(PathContainer.parsePath("/api-docs/swagger-config"))).isFalse();
		assertThat(pattern.matches(PathContainer.parsePath("/swagger-ui/index.html"))).isFalse();
		assertThat(pattern.matches(PathContainer.parsePath("/actuator/health"))).isFalse();
	}
}
