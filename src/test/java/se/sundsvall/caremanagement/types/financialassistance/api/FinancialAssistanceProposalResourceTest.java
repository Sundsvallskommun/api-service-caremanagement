package se.sundsvall.caremanagement.types.financialassistance.api;

import java.math.BigDecimal;
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
import se.sundsvall.caremanagement.types.financialassistance.api.model.DecisionProposal;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentProposal;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ProposedPayment;
import se.sundsvall.caremanagement.types.financialassistance.service.DecisionProposalService;
import se.sundsvall.caremanagement.types.financialassistance.service.PaymentProposalService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAssistanceProposalResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String DECISION_PATH = "/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/decision-proposal";
	private static final String PAYMENT_PATH = "/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/payment-proposal";

	@MockitoBean
	private DecisionProposalService decisionProposalServiceMock;

	@MockitoBean
	private PaymentProposalService paymentProposalServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getDecisionProposal() {
		final var proposal = DecisionProposal.create().withOutcome("BIFALL").withEstimatedAmount(new BigDecimal("4250")).withReasonOptions(List.of("Försörjningsstöd"));
		when(decisionProposalServiceMock.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(proposal);

		final var response = webTestClient.get()
			.uri(uri -> uri.path(DECISION_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(DecisionProposal.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(proposal);
		verify(decisionProposalServiceMock).get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verifyNoInteractions(paymentProposalServiceMock);
	}

	@Test
	void getPaymentProposal() {
		final var proposal = PaymentProposal.create().withPayeeSource("PREVIOUS_PAYMENT").withPayments(List.of(ProposedPayment.create().withConcernedMonth("2026-06")));
		when(paymentProposalServiceMock.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(proposal);

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PAYMENT_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(PaymentProposal.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(proposal);
		verify(paymentProposalServiceMock).get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verifyNoInteractions(decisionProposalServiceMock);
	}
}
