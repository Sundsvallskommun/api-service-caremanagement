package se.sundsvall.caremanagement.lifecare.professionalweb.integrator;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFamilyCare;
import se.sundsvall.caremanagement.lifecare.integration.integrator.LifecareIntegratorIntegration;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebTransport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProfessionalWeb is only reached through lifecare-integrator's exchange: the application starts without any of the
 * direct route's settings, and no property selects a route. The old provider properties of FamilyCare and
 * ProfessionalWeb share one context here, so both routes are checked against them at once.
 */
class ProfessionalWebRouteWiringTest {

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
		}
	}

	@Nested
	@SpringBootTest(classes = Application.class, properties = {
		"integration.lifecare-familycare.provider=familycare",
		"integration.lifecare-professionalweb.provider=direct"
	})
	@ActiveProfiles("junit")
	class WithTheOldProviderPropertiesSet {

		@Autowired
		private ApplicationContext context;

		@Test
		void theIntegratorIsUsedAnyway() {
			assertThat(context.getBeansOfType(LifecareFamilyCare.class)).hasSize(1);
			assertThat(context.getBean(LifecareFamilyCare.class)).isInstanceOf(LifecareIntegratorIntegration.class);
			assertThat(context.getBeansOfType(ProfessionalWebTransport.class)).hasSize(1);
			assertThat(context.getBean(ProfessionalWebTransport.class)).isInstanceOf(IntegratorProfessionalWebTransport.class);
		}
	}
}
