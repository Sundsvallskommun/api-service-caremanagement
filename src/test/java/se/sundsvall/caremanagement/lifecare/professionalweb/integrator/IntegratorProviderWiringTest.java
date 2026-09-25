package se.sundsvall.caremanagement.lifecare.professionalweb.integrator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.lifecare.professionalweb.DirectProfessionalWebTransport;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebTransport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * With provider=integrator the ProfessionalWeb calls go through the integrator, and careM holds no Lifecare session.
 */
@SpringBootTest(classes = Application.class, properties = "integration.lifecare-professionalweb.provider=integrator")
@ActiveProfiles("junit")
class IntegratorProviderWiringTest {

	@Autowired
	private ApplicationContext context;

	@Test
	void integratorTransportIsTheOnlyOne() {
		assertThat(context.getBean(ProfessionalWebTransport.class)).isInstanceOf(IntegratorProfessionalWebTransport.class);
		assertThat(context.getBeanNamesForType(DirectProfessionalWebTransport.class)).isEmpty();
		assertThat(context.containsBean("lifecareProfessionalWeb")).isFalse();
	}
}
