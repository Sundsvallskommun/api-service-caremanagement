package se.sundsvall.caremanagement.lifecare.professionalweb.integrator;

import java.util.Arrays;
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
	void theProfessionalWebClientIsTheIntegratorsResources() {
		assertThat(context.getBeansOfType(ProfessionalWebIntegratorClient.class)).hasSize(1);
	}

	@Test
	void noClientCallsTheExchange() {
		final var paths = Arrays.stream(context.getBeanNamesForAnnotation(FeignClient.class))
			.map(context::getType)
			.flatMap(type -> Stream.concat(Stream.of(type), Arrays.stream(type.getInterfaces())))
			.filter(type -> type.isAnnotationPresent(FeignClient.class))
			.distinct()
			.flatMap(type -> Arrays.stream(type.getMethods()))
			.map(method -> AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class))
			.filter(mapping -> mapping != null)
			.flatMap(mapping -> Arrays.stream(mapping.path()))
			.toList();

		assertThat(paths).isNotEmpty().noneMatch(path -> path.contains("/professional-web/exchange"));
	}
}
