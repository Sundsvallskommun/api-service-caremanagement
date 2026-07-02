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

class ErrandEventWebConfigurationTest {

	@Test
	void registersErrandEventInterceptor() {
		final var configuration = new ErrandEventWebConfiguration(
			new ErrandEventInterceptor(mock(ErrandEventService.class)));
		final var registry = new InterceptorRegistry();

		configuration.addInterceptors(registry);

		final List<?> interceptors = ReflectionTestUtils.invokeMethod(registry, "getInterceptors");
		assertThat(interceptors).hasSize(1);
	}

	@Test
	void errandPatternMatchesOnlyNumericMunicipalityErrandPaths() {
		final var pattern = new PathPatternParser().parse(ErrandEventWebConfiguration.ERRAND_PATTERN);

		// real errand-scoped paths are gated
		assertThat(pattern.matches(PathContainer.parsePath("/2281/FINANCIAL_ASSISTANCE/errands/abc"))).isTrue();

		// non-errand and infra / non-numeric first segments must NOT be gated (so api docs + swagger keep working)
		assertThat(pattern.matches(PathContainer.parsePath("/2281/FINANCIAL_ASSISTANCE/metadata"))).isFalse();
		assertThat(pattern.matches(PathContainer.parsePath("/api-docs"))).isFalse();
		assertThat(pattern.matches(PathContainer.parsePath("/api-docs/swagger-config"))).isFalse();
		assertThat(pattern.matches(PathContainer.parsePath("/swagger-ui/index.html"))).isFalse();
		assertThat(pattern.matches(PathContainer.parsePath("/actuator/health"))).isFalse();
	}
}
