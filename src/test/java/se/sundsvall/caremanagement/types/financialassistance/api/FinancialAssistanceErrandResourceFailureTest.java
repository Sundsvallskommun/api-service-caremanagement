package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.MultipartBodyBuilder;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateFinancialAssistanceRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceData;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

class FinancialAssistanceErrandResourceFailureTest extends AbstractFinancialAssistanceResourceTest {

	@Test
	void createErrandBlankTitle() {
		final var builder = new MultipartBodyBuilder();
		builder.part("request", CreateFinancialAssistanceRequest.create().withTitle(" ").withData(FinancialAssistanceData.create()), APPLICATION_JSON);

		webTestClient.post()
			.uri(uri -> uri.path(CREATE_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("title", "must not be blank")));

		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void createErrandMissingData() {
		final var builder = new MultipartBodyBuilder();
		builder.part("request", CreateFinancialAssistanceRequest.create().withTitle("ok"), APPLICATION_JSON);

		webTestClient.post()
			.uri(uri -> uri.path(CREATE_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("data", "must not be null")));

		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void createErrandMalformedRequestJson() {
		final var builder = new MultipartBodyBuilder();
		builder.part("request", "{not valid json", APPLICATION_JSON);

		webTestClient.post()
			.uri(uri -> uri.path(CREATE_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody()
			.jsonPath("$.title").isEqualTo("Bad Request")
			.jsonPath("$.status").isEqualTo(400)
			.jsonPath("$.detail").isEqualTo("The 'request' part could not be read as a financial-assistance application — check that it is valid JSON matching the schema.");

		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void createErrandInvalidApplicationType() {
		final var builder = new MultipartBodyBuilder();
		builder.part("request", CreateFinancialAssistanceRequest.create().withTitle("ok").withData(FinancialAssistanceData.create().withApplicationType("BOGUS")), APPLICATION_JSON);

		webTestClient.post()
			.uri(uri -> uri.path(CREATE_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("data.applicationType", "must be one of: [NEW, RENEWAL, SUPPLEMENTARY]")));

		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void readErrandInvalidErrandId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{errandId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readErrand.errandId", "not a valid UUID")));

		verifyNoInteractions(errandServiceMock);
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
