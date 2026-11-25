package se.sundsvall.caremanagement.lifecare.professionalweb.integrator;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.RequestMapping;
import se.sundsvall.caremanagement.Application;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProfessionalWeb is reached only through lifecare-integrator's resources: no Feign client in the application calls the
 * integrator's generic exchange.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class ProfessionalWebRouteWiringTest {

	@Autowired
	private ApplicationContext context;

	@Test
	void noClientCallsTheExchange() {
		final var feignClients = Arrays.stream(context.getBeanNamesForAnnotation(FeignClient.class))
			.map(context::getType)
			.flatMap(type -> Stream.concat(Stream.of(type), Arrays.stream(type.getInterfaces())))
			.filter(type -> type.isAnnotationPresent(FeignClient.class))
			.distinct()
			.toList();
		final var paths = feignClients.stream()
			.flatMap(type -> Arrays.stream(type.getMethods()))
			.map(method -> AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class))
			.filter(Objects::nonNull)
			.flatMap(mapping -> Arrays.stream(mapping.path()))
			.toList();

		assertThat(feignClients).contains(ProfessionalWebIntegratorClient.class);
		assertThat(paths).noneMatch(path -> path.contains("/professional-web/exchange"));
	}
}
