package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.MultipartBodyBuilder;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ActualisationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ArchiveActualisationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.EligibilityRequest;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

class FinancialAssistanceIntakeResourceFailureTest extends AbstractFinancialAssistanceResourceTest {

	@Test
	void checkEligibilityMissingApplicant() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/eligibility").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(EligibilityRequest.create())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("applicant", "not a valid UUID")));

		verifyNoInteractions(eligibilityServiceMock);
	}

	@Test
	void checkEligibilityInvalidApplicant() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/eligibility").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(EligibilityRequest.create().withApplicant("123"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("applicant", "not a valid UUID")));

		verifyNoInteractions(eligibilityServiceMock);
	}

	@Test
	void checkEligibilityInvalidCoApplicant() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/eligibility").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(EligibilityRequest.create().withApplicant("f47ac10b-58cc-4372-a567-0e02b2c3d479").withCoApplicant("nope"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("coApplicant", "not a valid UUID")));

		verifyNoInteractions(eligibilityServiceMock);
	}

	@Test
	void createActualisationInvalidApplicant() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/actualisation").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(ActualisationRequest.create().withApplicant("123").withApplicationMonth("2026-06"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("applicant", "not a valid UUID")));

		verifyNoInteractions(actualisationServiceMock);
	}

	@Test
	void createActualisationInvalidMonth() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/actualisation").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(ActualisationRequest.create().withApplicant("f47ac10b-58cc-4372-a567-0e02b2c3d479").withApplicationMonth("2026-13"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("applicationMonth", "must be an ISO year-month (yyyy-MM)")));

		verifyNoInteractions(actualisationServiceMock);
	}

	@Test
	void prefillInvalidPartyId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/prefill").queryParam("partyId", "not-a-uuid").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("prefill.partyId", "not a valid UUID")));

		verifyNoInteractions(prefillServiceMock);
	}

	@Test
	void listActualisationsInvalidPartyId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/actualisations").queryParam("partyId", "not-a-uuid").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("listActualisations.partyId", "not a valid UUID")));

		verifyNoInteractions(actualisationServiceMock);
	}

	@Test
	void listActualisationsInvalidMunicipalityId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/actualisations").queryParam("partyId", randomUUID().toString()).build(Map.of("municipalityId", "x", "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("listActualisations.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(actualisationServiceMock);
	}

	@Test
	void archiveToActualisationInvalidMunicipalityId() {
		final var builder = new MultipartBodyBuilder();
		builder.part("file", "%PDF-1.4".getBytes()).filename("tillaggsansokan.pdf");

		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/actualisations/{actualisationId}/archive").queryParam("partyId", "f47ac10b-58cc-4372-a567-0e02b2c3d479").build(Map.of("municipalityId", "x", "namespace", NAMESPACE, "actualisationId", 5012)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("archiveToActualisation.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(actualisationServiceMock);
	}

	@Test
	void archiveToActualisationMissingFile() {
		final var builder = new MultipartBodyBuilder();
		builder.part("request", ArchiveActualisationRequest.create().withTitle("x"), APPLICATION_JSON);

		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/actualisations/{actualisationId}/archive").queryParam("partyId", "f47ac10b-58cc-4372-a567-0e02b2c3d479").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "actualisationId", 5012)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody()
			.jsonPath("$.title").isEqualTo("Bad Request")
			.jsonPath("$.status").isEqualTo(400)
			.jsonPath("$.detail").isEqualTo("Required part 'file' is not present.");

		verifyNoInteractions(actualisationServiceMock);
	}

	@Test
	void archiveToActualisationInvalidErrandId() {
		final var builder = new MultipartBodyBuilder();
		builder.part("file", "%PDF-1.4".getBytes()).filename("tillaggsansokan.pdf");
		builder.part("request", ArchiveActualisationRequest.create().withErrandId("not-a-uuid"), APPLICATION_JSON);

		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/actualisations/{actualisationId}/archive").queryParam("partyId", "f47ac10b-58cc-4372-a567-0e02b2c3d479").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "actualisationId", 5012)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("errandId", "not a valid UUID")));

		verifyNoInteractions(actualisationServiceMock);
	}

	@Test
	void archiveToActualisationInvalidPartyId() {
		final var builder = new MultipartBodyBuilder();
		builder.part("file", "%PDF-1.4".getBytes()).filename("tillaggsansokan.pdf");

		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/actualisations/{actualisationId}/archive").queryParam("partyId", "not-a-uuid").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "actualisationId", 5012)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("archiveToActualisation.partyId", "not a valid UUID")));

		verifyNoInteractions(actualisationServiceMock);
	}

	private static void assertConstraintViolation(final ConstraintViolationProblem response, final Tuple... violations) {
		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.isNotEmpty()
			.allSatisfy(violation -> assertThat(violation.field()).isNotBlank())
			.allSatisfy(violation -> assertThat(violation.message()).isNotBlank());
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(violations);
	}
}
