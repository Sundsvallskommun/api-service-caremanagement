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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentFixtures.json;

@ExtendWith(MockitoExtension.class)
class LifecarePaymentApiTest {

	@Mock
	private ProfessionalWebClient client;

	@InjectMocks
	private LifecarePaymentApi api;

	@Test
	void readPaymentForCreate() {
		final var answer = json("{}");
		when(client.readPaymentProposal(24)).thenReturn(answer);

		assertThat(api.readPaymentForCreate(24)).isSameAs(answer);
	}

	@Test
	void readLatestPayments() {
		final var answer = json("[]");
		when(client.readLatestPayments(24)).thenReturn(answer);

		assertThat(api.readLatestPayments(24)).isSameAs(answer);
	}

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

	@Test
	void createPayment() {
		final var body = json("{}");
		final var answer = json("{ \"paymentId\": 4 }");
		when(client.createPayment(24, body)).thenReturn(answer);

		assertThat(api.createPayment(24, body)).isSameAs(answer);
		verify(client).createPayment(24, body);
	}

	@Test
	void createPayee() {
		final var body = json("{}");
		final var answer = json("{ \"payeeId\": 3 }");
		when(client.createPayee(body)).thenReturn(answer);

		assertThat(api.createPayee(body)).isSameAs(answer);
	}
}
