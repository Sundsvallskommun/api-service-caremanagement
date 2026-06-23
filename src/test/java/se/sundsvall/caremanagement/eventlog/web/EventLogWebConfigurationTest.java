package se.sundsvall.caremanagement.eventlog.web;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
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
}
