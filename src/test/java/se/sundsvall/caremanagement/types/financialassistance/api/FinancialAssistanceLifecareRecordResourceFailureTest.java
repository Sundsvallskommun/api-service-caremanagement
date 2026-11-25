package se.sundsvall.caremanagement.types.financialassistance.api;

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
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.CreateLifecareDocumentRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.CreateLifecareJournalNoteRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.UpdateLifecareRecordRequest;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareRecordService;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PDF;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAssistanceLifecareRecordResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "cb20c51f-fcf3-42c0-b613-de563634a8ec";
	private static final String INVALID_MUNICIPALITY_ID = "bad-municipality-id";
	private static final String INVALID_UUID = "not-a-valid-uuid";
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/lifecare";
	private static final Map<String, Object> URI_VARIABLES = Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID);

	@MockitoBean
	private LifecareRecordService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	private static void assertConstraintViolation(final ConstraintViolationProblem response, final Tuple... violations) {
		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(violations);
	}

	@Test
	void listRecordsWithInvalidMunicipalityId() {
		webTestClient.get()
			.uri(builder -> builder.path(PATH + "/documents").build(Map.of("municipalityId", INVALID_MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("listRecords.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void listJournalNoteBodiesWithInvalidErrandId() {
		webTestClient.get()
			.uri(builder -> builder.path(PATH + "/journal-note-bodies").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", INVALID_UUID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("listJournalNoteBodies.errandId", "not a valid UUID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createJournalNoteWithInvalidBody() {
		webTestClient.post()
			.uri(builder -> builder.path(PATH + "/documents/journal-notes").build(URI_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(CreateLifecareJournalNoteRequest.create().withContent("").withOccurenceTime("9:2").withOccurenceDate("2026-9-1"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("content", "must not be blank"),
				tuple("noteTypeCode", "must not be null"),
				tuple("occurenceTime", "must be HH:mm"),
				tuple("occurenceDate", "must be YYYY-MM-DD")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createDocumentWithInvalidBody() {
		webTestClient.post()
			.uri(builder -> builder.path(PATH + "/documents/documents").build(URI_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(CreateLifecareDocumentRequest.create().withContent("<p>Hej</p>").withTitle("x".repeat(256)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("documentTypeCode", "must not be null"),
				tuple("title", "size must be between 0 and 255")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateJournalNoteWithInvalidBody() {
		webTestClient.put()
			.uri(builder -> builder.path(PATH + "/documents/journal-notes/{id}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE,
				"errandId", ERRAND_ID, "id", 138)))
			.contentType(APPLICATION_JSON)
			.bodyValue(UpdateLifecareRecordRequest.create().withTime("nine"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("content", "must not be null"),
				tuple("time", "must be HH:mm")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readDocumentWithInvalidId() {
		webTestClient.get()
			.uri(builder -> builder.path(PATH + "/documents/documents/{id}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE,
				"errandId", ERRAND_ID, "id", -1)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readDocument.id", "must be greater than 0")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readDocumentPdfWithInvalidErrandId() {
		webTestClient.get()
			.uri(builder -> builder.path(PATH + "/documents/documents/{id}/pdf").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE,
				"errandId", "not-a-uuid", "id", 138)))
			.accept(APPLICATION_PDF, APPLICATION_PROBLEM_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readDocumentPdf.errandId", "not a valid UUID")));

		verifyNoInteractions(serviceMock);
	}
}
