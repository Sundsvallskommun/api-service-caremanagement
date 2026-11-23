package se.sundsvall.caremanagement.lifecare;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.caremanagement.Application;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * careM reports nothing about Lifecare on its health endpoint: there is no ProfessionalWeb session of its own to
 * report on, and no indicator against lifecare-integrator took its place.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class LifecareHealthTest {

	@Autowired
	private ApplicationContext context;

	@Test
	void noHealthContributorConcernsLifecare() {
		assertThat(context.getBeansOfType(HealthContributor.class).keySet())
			.isNotEmpty()
			.noneMatch(name -> name.toLowerCase().contains("lifecare"))
			.noneMatch(name -> name.toLowerCase().contains("professionalweb"))
			.noneMatch(name -> name.toLowerCase().contains("integrator"));
		assertThat(context.containsBean("lifecareProfessionalWeb")).isFalse();
	}
}
