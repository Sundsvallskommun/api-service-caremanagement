package se.sundsvall.caremanagement.lifecare.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.lifecare.integration.integrator.LifecareIntegratorIntegration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FamilyCare is only reached through lifecare-integrator: the application starts without any of the direct route's
 * settings, and no property selects a route.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class FamilyCareRouteWiringTest {

	@Autowired
	private ApplicationContext context;

	@Test
	void theOnlyFamilyCareImplementationCallsTheIntegrator() {
		assertThat(context.getBeansOfType(LifecareFamilyCare.class)).hasSize(1);
		assertThat(context.getBean(LifecareFamilyCare.class)).isInstanceOf(LifecareIntegratorIntegration.class);
	}
}
