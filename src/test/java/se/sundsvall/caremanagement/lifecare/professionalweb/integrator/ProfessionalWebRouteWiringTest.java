package se.sundsvall.caremanagement.lifecare.professionalweb.integrator;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebTransport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProfessionalWeb is only reached through lifecare-integrator's exchange: the junit profile sets none of the direct
 * route's settings (url, domain, federation-profile, username, password), and no property selects a route.
 */
class ProfessionalWebRouteWiringTest {

	private static final String[] DIRECT_ROUTE_SETTINGS = {
		"provider", "url", "domain", "federation-profile", "username", "password"
	};

	@Nested
	@SpringBootTest(classes = Application.class)
	@ActiveProfiles("junit")
	class WithoutAnyRouteSettings {

		@Autowired
		private ApplicationContext context;

		@Test
		void theOnlyTransportIsTheIntegratorsExchange() {
			assertThat(context.getBeansOfType(ProfessionalWebTransport.class)).hasSize(1);
			assertThat(context.getBean(ProfessionalWebTransport.class)).isInstanceOf(IntegratorProfessionalWebTransport.class);
			for (final var setting : DIRECT_ROUTE_SETTINGS) {
				assertThat(context.getEnvironment().getProperty("integration.lifecare-professionalweb." + setting)).as(setting).isNullOrEmpty();
			}
		}
	}

	@Nested
	@SpringBootTest(classes = Application.class, properties = "integration.lifecare-professionalweb.provider=direct")
	@ActiveProfiles("junit")
	class WithTheOldProviderPropertySet {

		@Autowired
		private ApplicationContext context;

		@Test
		void theIntegratorIsUsedAnyway() {
			assertThat(context.getBeansOfType(ProfessionalWebTransport.class)).hasSize(1);
			assertThat(context.getBean(ProfessionalWebTransport.class)).isInstanceOf(IntegratorProfessionalWebTransport.class);
		}
	}
}
