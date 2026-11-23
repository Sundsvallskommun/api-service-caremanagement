package se.sundsvall.caremanagement.lifecare.integration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.lifecare.integration.integrator.LifecareIntegratorIntegration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FamilyCare is only reached through lifecare-integrator: the junit profile sets none of the direct route's settings
 * (url, domain, key), and no property selects a route.
 */
class FamilyCareRouteWiringTest {

	@Nested
	@SpringBootTest(classes = Application.class)
	@ActiveProfiles("junit")
	class WithoutAnyRouteSettings {

		@Autowired
		private ApplicationContext context;

		@Test
		void theOnlyFamilyCareImplementationCallsTheIntegrator() {
			assertThat(context.getBeansOfType(LifecareFamilyCare.class)).hasSize(1);
			assertThat(context.getBean(LifecareFamilyCare.class)).isInstanceOf(LifecareIntegratorIntegration.class);
			assertThat(context.getEnvironment().getProperty("integration.lifecare-familycare.url")).isNull();
			assertThat(context.getEnvironment().getProperty("integration.lifecare-familycare.domain")).isNull();
			assertThat(context.getEnvironment().getProperty("integration.lifecare-familycare.key")).isNull();
		}
	}

	@Nested
	@SpringBootTest(classes = Application.class, properties = "integration.lifecare-familycare.provider=familycare")
	@ActiveProfiles("junit")
	class WithTheOldProviderPropertySet {

		@Autowired
		private ApplicationContext context;

		@Test
		void theIntegratorIsUsedAnyway() {
			assertThat(context.getBeansOfType(LifecareFamilyCare.class)).hasSize(1);
			assertThat(context.getBean(LifecareFamilyCare.class)).isInstanceOf(LifecareIntegratorIntegration.class);
		}
	}
}
