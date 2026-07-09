package se.sundsvall.caremanagement.document.api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.document.api.model.CreateDocument;
import se.sundsvall.caremanagement.document.service.DocumentService;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static se.sundsvall.caremanagement.support.ConstraintViolationAssertions.assertConstraintViolation;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class DocumentResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/documents";
	private static final LocalDate DOCUMENT_DATE = LocalDate.parse("2025-05-30");
	private static final LocalTime DOCUMENT_TIME = LocalTime.of(14, 30);

	@MockitoBean
	private DocumentService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void addWithBlankType() {
		post(new CreateDocument(" ", "Rubrik", "body", DOCUMENT_DATE, DOCUMENT_TIME, "carola"),
			tuple("type", "must not be blank"));
	}

	@Test
	void addWithBlankHeading() {
		post(new CreateDocument("Brev", " ", "body", DOCUMENT_DATE, DOCUMENT_TIME, "carola"),
			tuple("heading", "must not be blank"));
	}

	@Test
	void addWithMissingDocumentDate() {
		post(new CreateDocument("Brev", "Rubrik", "body", null, DOCUMENT_TIME, "carola"),
			tuple("documentDate", "must not be null"));
	}

	@Test
	void addWithInvalidErrandId() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.bodyValue(new CreateDocument("Brev", "Rubrik", "body", DOCUMENT_DATE, DOCUMENT_TIME, "carola"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createDocument.errandId", "not a valid UUID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void addWithInvalidMunicipalityId() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", "invalid", "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(new CreateDocument("Brev", "Rubrik", "body", DOCUMENT_DATE, DOCUMENT_TIME, "carola"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createDocument.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(serviceMock);
	}

	private void post(final CreateDocument request, final org.assertj.core.groups.Tuple... violations) {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(request)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(), violations));

		verifyNoInteractions(serviceMock);
	}
}
