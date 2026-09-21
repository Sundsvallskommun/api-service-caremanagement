package se.sundsvall.caremanagement.types.financialassistance.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.assertj.core.groups.Tuple;
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
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizePayment;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Payee;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceFinalizeService;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;
import se.sundsvall.dept44.support.Identifier;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAssistanceFinalizeResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/finalize";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private FinancialAssistanceFinalizeService finalizeServiceMock;

	private static FinalizeRequest validRequest() {
		return FinalizeRequest.create()
			.withDecision(FinalizeDecision.create()
				.withOutcome("BIFALL")
				.withPeriodFrom(LocalDate.of(2026, 6, 1))
				.withPeriodTo(LocalDate.of(2026, 6, 30))
				.withAmount(new BigDecimal("7900.00")))
			.withCommunication(CommunicationChannels.create().withMinaSidor(true).withDigitalMailbox(false).withLetter(false))
			.withPayments(List.of(validPayment()))
			.withHouseholdSizeChanged(false);
	}

	private static FinalizePayment validPayment() {
		return FinalizePayment.create()
			.withPaymentDate(LocalDate.of(2026, 6, 25))
			.withAmount(new BigDecimal("7900.00"))
			.withConcernedMonth("2026-06")
			.withPayee(Payee.create().withName("Anna Andersson").withPaymentMethod("BANKKONTO"));
	}

	private static void assertConstraintViolation(final ConstraintViolationProblem response, final Tuple... violations) {
		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(violations);
	}

	private ConstraintViolationProblem post(final String municipalityId, final String errandId, final FinalizeRequest request) {
		return webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", municipalityId, "namespace", NAMESPACE, "errandId", errandId)))
			.header(Identifier.HEADER_NAME, "jane02doe; type=adAccount")
			.contentType(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();
	}

	@Test
	void invalidMunicipalityId() {
		assertConstraintViolation(post("x", ERRAND_ID, validRequest()),
			tuple("finalize.municipalityId", "not a valid municipality ID"));
		verifyNoInteractions(finalizeServiceMock);
	}

	@Test
	void invalidErrandId() {
		assertConstraintViolation(post(MUNICIPALITY_ID, "not-a-uuid", validRequest()),
			tuple("finalize.errandId", "not a valid UUID"));
		verifyNoInteractions(finalizeServiceMock);
	}

	@Test
	void missingDecisionAndCommunication() {
		assertConstraintViolation(post(MUNICIPALITY_ID, ERRAND_ID, FinalizeRequest.create()),
			tuple("decision", "must not be null"),
			tuple("communication", "must not be null"));
		verifyNoInteractions(finalizeServiceMock);
	}

	@Test
	void invalidOutcomeAndNegativeAmount() {
		final var request = validRequest();
		request.getDecision().withOutcome("BEVILJAD").withAmount(new BigDecimal("-1"));

		assertConstraintViolation(post(MUNICIPALITY_ID, ERRAND_ID, request),
			tuple("decision.outcome", "must be one of: [BIFALL, DELAVSLAG, AVSLAG, AVVISNING]"),
			tuple("decision.amount", "must be greater than or equal to 0"));
		verifyNoInteractions(finalizeServiceMock);
	}

	@Test
	void grantingOutcomeWithoutAmountOrPayments() {
		final var request = validRequest().withPayments(List.of());
		request.getDecision().withAmount(null);

		assertConstraintViolation(post(MUNICIPALITY_ID, ERRAND_ID, request),
			tuple("decision.amount", "must be given when the outcome carries an amount (BIFALL/DELAVSLAG)"),
			tuple("payments", "at least one payment is required when the outcome carries an amount (BIFALL/DELAVSLAG)"));
		verifyNoInteractions(finalizeServiceMock);
	}

	@Test
	void nonGrantingOutcomeWithPayments() {
		final var request = validRequest();
		request.getDecision().withOutcome("AVSLAG");

		assertConstraintViolation(post(MUNICIPALITY_ID, ERRAND_ID, request),
			tuple("payments", "must be empty when the outcome carries no amount (AVSLAG/AVVISNING)"));
		verifyNoInteractions(finalizeServiceMock);
	}

	@Test
	void periodEndingBeforeStart() {
		final var request = validRequest();
		request.getDecision().withPeriodFrom(LocalDate.of(2026, 6, 30)).withPeriodTo(LocalDate.of(2026, 6, 1));

		assertConstraintViolation(post(MUNICIPALITY_ID, ERRAND_ID, request),
			tuple("decision.periodTo", "must not be before periodFrom"));
		verifyNoInteractions(finalizeServiceMock);
	}

	@Test
	void missingCommunicationChannelFlags() {
		final var request = validRequest().withCommunication(CommunicationChannels.create());

		assertConstraintViolation(post(MUNICIPALITY_ID, ERRAND_ID, request),
			tuple("communication.minaSidor", "must not be null"),
			tuple("communication.digitalMailbox", "must not be null"),
			tuple("communication.letter", "must not be null"));
		verifyNoInteractions(finalizeServiceMock);
	}

	@Test
	void invalidPayment() {
		final var request = validRequest().withPayments(List.of(FinalizePayment.create()
			.withAmount(new BigDecimal("0"))
			.withConcernedMonth("2026-13")
			.withPayee(Payee.create())));

		assertConstraintViolation(post(MUNICIPALITY_ID, ERRAND_ID, request),
			tuple("payments[0].paymentDate", "must not be null"),
			tuple("payments[0].amount", "must be greater than 0"),
			tuple("payments[0].concernedMonth", "must be an ISO year-month (yyyy-MM)"),
			tuple("payments[0].payee.name", "must not be blank"),
			tuple("payments[0].payee.paymentMethod", "must not be blank"));
		verifyNoInteractions(finalizeServiceMock);
	}

	@Test
	void missingPayee() {
		final var request = validRequest().withPayments(List.of(validPayment().withPayee(null)));

		assertConstraintViolation(post(MUNICIPALITY_ID, ERRAND_ID, request),
			tuple("payments[0].payee", "must not be null"));
		verifyNoInteractions(finalizeServiceMock);
	}

	@Test
	void serviceConflictIsPassedThrough() {
		final var request = validRequest();
		when(finalizeServiceMock.finalize(any(), any(), any(), any(), any()))
			.thenThrow(Problem.valueOf(CONFLICT, "errand must be in status AWAITING_DECISION to be finalized, but is in status 'UNDER_REVIEW'"));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.header(Identifier.HEADER_NAME, "jane02doe; type=adAccount")
			.contentType(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isEqualTo(CONFLICT)
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(CONFLICT);
		assertThat(response.getDetail()).contains("AWAITING_DECISION");
		verify(finalizeServiceMock).finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request, "jane02doe");
	}
}
