package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.util.Map;
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

	private static final Map<String, String> SERVICE = Map.of("businessType", "8", "businessId", "24");

	@Mock
	private ProfessionalWebClient client;

	@InjectMocks
	private LifecarePaymentApi api;

	@Test
	void readPaymentForCreate() {
		final var answer = json("{}");
		when(client.get("api2/Payment/GetPaymentForCreate", SERVICE)).thenReturn(answer);

		assertThat(api.readPaymentForCreate(24)).isSameAs(answer);
	}

	@Test
	void readLatestPayments() {
		final var answer = json("[]");
		when(client.get("api2/Payment/GetLatestPayments", SERVICE)).thenReturn(answer);

		assertThat(api.readLatestPayments(24)).isSameAs(answer);
	}

	@Test
	void hasHouseholdOn() {
		when(client.get("api2/Household/HasHouseholdThisDate/", Map.of("personId", "19800101T001", "date", "2026-09-21"))).thenReturn(BooleanNode.TRUE);
		when(client.get("api2/Household/HasHouseholdThisDate/", Map.of("personId", "19800101T001", "date", "2026-09-22"))).thenReturn(MissingNode.getInstance());

		assertThat(api.hasHouseholdOn("19800101T001", "2026-09-21")).isTrue();
		assertThat(api.hasHouseholdOn("19800101T001", "2026-09-22")).isFalse();
	}

	@Test
	void createPayment() {
		final var body = json("{}");
		final var answer = json("{ \"paymentId\": 4 }");
		when(client.post("api2/Payment/Create", SERVICE, body)).thenReturn(answer);

		assertThat(api.createPayment(24, body)).isSameAs(answer);
		verify(client).post("api2/Payment/Create", SERVICE, body);
	}

	@Test
	void createPayee() {
		final var body = json("{}");
		final var answer = json("{ \"payeeId\": 3 }");
		when(client.post("api2/Payee/Create", Map.of(), body)).thenReturn(answer);

		assertThat(api.createPayee(body)).isSameAs(answer);
	}
}
