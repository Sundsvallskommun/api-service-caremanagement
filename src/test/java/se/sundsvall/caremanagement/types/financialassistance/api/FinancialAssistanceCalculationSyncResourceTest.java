package se.sundsvall.caremanagement.types.financialassistance.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.types.financialassistance.api.model.AppliedSsbtekChange;
import se.sundsvall.caremanagement.types.financialassistance.api.model.AppliedSsbtekChanges;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SsbtekChange;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SsbtekChanges;
import se.sundsvall.caremanagement.types.financialassistance.service.CalculationSyncService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAssistanceCalculationSyncResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/calculation/ssbtek-changes";

	@MockitoBean
	private CalculationSyncService calculationSyncServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getChanges() {
		final var changes = new SsbtekChanges(4242, false, OffsetDateTime.parse("2026-09-25T09:30:00+02:00"), OffsetDateTime.parse("2026-09-25T03:00:00+02:00"),
			List.of(new SsbtekChange("CHANGE", "AUTO", null, "APPLICANT", 20, "Lön", new BigDecimal("12400.00"), new BigDecimal("11900.00"))));
		when(calculationSyncServiceMock.changes(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(changes);

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(SsbtekChanges.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.calculationId()).isEqualTo(4242);
		assertThat(response.isFinal()).isFalse();
		assertThat(response.changes()).singleElement().satisfies(change -> {
			assertThat(change.mode()).isEqualTo("AUTO");
			assertThat(change.ssbtekAmount()).isEqualByComparingTo("12400");
		});
		verify(calculationSyncServiceMock).changes(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void applied() {
		final var request = new AppliedSsbtekChanges(4242, List.of(new AppliedSsbtekChange("APPLICANT", "Lön", new BigDecimal("12400")),
			new AppliedSsbtekChange("CO_APPLICANT", "Barnbidrag", null)));

		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/applied").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(request)
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		verify(calculationSyncServiceMock).applied(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request);
	}
}
