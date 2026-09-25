package se.sundsvall.caremanagement.types.financialassistance.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CommunicationChannels;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeDecision;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDecisionRegistration;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareFinalizeService;
import se.sundsvall.dept44.support.Identifier;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAssistanceFinalizeResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/finalize";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private LifecareFinalizeService finalizeServiceMock;

	private static FinalizeRequest validRequest() {
		return FinalizeRequest.create()
			.withDecision(FinalizeDecision.create()
				.withOutcome("BIFALL")
				.withReason("Inkomster enligt SSBTEK")
				.withPeriodFrom(LocalDate.of(2026, 6, 1))
				.withPeriodTo(LocalDate.of(2026, 6, 30))
				.withAmount(new BigDecimal("7900.00"))
				.withDecisionMessage("Du beviljas ekonomiskt bistånd för juni 2026"))
			.withCommunication(CommunicationChannels.create().withMinaSidor(true).withDigitalMailbox(false).withLetter(false))
			.withHouseholdSizeChanged(false);
	}

	@Test
	void finalizePassesTheCallerIdentityAsDecider() {
		final var request = validRequest();
		final var expected = FinalizeResponse.create()
			.withDecisionId("decision-1")
			.withProcessMessageCorrelated(true)
			.withCommunication(request.getCommunication());
		when(finalizeServiceMock.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request, "jane02doe")).thenReturn(expected);

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.header(Identifier.HEADER_NAME, "jane02doe; type=adAccount") // the decider comes from X-Sent-By, not the body
			.contentType(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isOk()
			.expectBody(FinalizeResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(expected);
		verify(finalizeServiceMock).finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request, "jane02doe");
	}

	@Test
	void finalizeWithoutIdentityPassesNullDecider() {
		// The service rejects a missing decider with 400 — the resource only forwards what it got.
		final var request = validRequest();
		when(finalizeServiceMock.finalize(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(request), isNull())).thenReturn(FinalizeResponse.create());

		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isOk();

		verify(finalizeServiceMock).finalize(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(request), isNull());
	}

	@Test
	void paymentsFromAClientThatPredatesTheLifecareReferencesAreIgnored() {
		// Draken before the change still sends its payment drafts; careM no longer reads them and creates no rows.
		final var body = """
			{"decision": {"outcome": "AVSLAG"},
			 "communication": {"minaSidor": true, "digitalMailbox": false, "letter": false},
			 "payments": [{"paymentDate": "2026-06-25", "amount": 7900, "concernedMonth": "2026-06", "payee": {"name": "Anna Andersson", "paymentMethod": "BANKKONTO"}}]}
			""";
		final var expected = FinalizeRequest.create()
			.withDecision(FinalizeDecision.create().withOutcome("AVSLAG"))
			.withCommunication(CommunicationChannels.create().withMinaSidor(true).withDigitalMailbox(false).withLetter(false));
		when(finalizeServiceMock.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, expected, "jane02doe")).thenReturn(FinalizeResponse.create());

		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.header(Identifier.HEADER_NAME, "jane02doe; type=adAccount")
			.contentType(APPLICATION_JSON)
			.bodyValue(body)
			.exchange()
			.expectStatus().isOk();

		verify(finalizeServiceMock).finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, expected, "jane02doe");
	}

	@Test
	void finalizeWithoutADecisionLeavesItToBeReadFromLifecare() {
		final var request = FinalizeRequest.create()
			.withCommunication(CommunicationChannels.create().withMinaSidor(true).withDigitalMailbox(false).withLetter(false));
		final var expected = FinalizeResponse.create()
			.withDecisionId("decision-1")
			.withProcessMessageCorrelated(true)
			.withLifecareDecision(new LifecareDecisionRegistration("decision-1", "REGISTERED", "98", null));
		when(finalizeServiceMock.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request, "jane02doe")).thenReturn(expected);

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.header(Identifier.HEADER_NAME, "jane02doe; type=adAccount")
			.contentType(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isOk()
			.expectBody(FinalizeResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(expected);
		verify(finalizeServiceMock).finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request, "jane02doe");
	}
}
