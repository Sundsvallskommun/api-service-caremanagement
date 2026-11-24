package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebClient;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.MissingNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LifecarePaymentApiTest {

	@Mock
	private ProfessionalWebClient client;

	@InjectMocks
	private LifecarePaymentApi api;

	@Test
	void hasHouseholdOnAsksWithTheInsatsAndTheDate() {
		when(client.hasHousehold(24, "2026-09-21")).thenReturn(BooleanNode.TRUE);
		when(client.hasHousehold(24, "2026-09-22")).thenReturn(BooleanNode.FALSE);

		assertThat(api.hasHouseholdOn(24, "2026-09-21")).isTrue();
		assertThat(api.hasHouseholdOn(24, "2026-09-22")).isFalse();
	}

	@Test
	void anEmptyHouseholdAnswerIsNoHousehold() {
		when(client.hasHousehold(24, "2026-09-21")).thenReturn(MissingNode.getInstance());

		assertThat(api.hasHouseholdOn(24, "2026-09-21")).isFalse();
	}
}
