package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.MultipartBodyBuilder;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ActualisationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.EligibilityRequest;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

class FinancialAssistanceIntakeResourceFailureTest extends AbstractFinancialAssistanceResourceTest {

	@Test
	void checkEligibility_missingApplicant() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/eligibility").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(EligibilityRequest.create())
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(eligibilityServiceMock);
	}

	@Test
	void checkEligibility_invalidApplicant() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/eligibility").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(EligibilityRequest.create().withApplicant("123"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(eligibilityServiceMock);
	}

	@Test
	void checkEligibility_invalidCoApplicant() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/eligibility").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(EligibilityRequest.create().withApplicant("f47ac10b-58cc-4372-a567-0e02b2c3d479").withCoApplicant("nope"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(eligibilityServiceMock);
	}

	@Test
	void createActualisation_invalidApplicant() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/actualisation").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(ActualisationRequest.create().withApplicant("123").withApplicationMonth("2026-06"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createActualisation_invalidMonth() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/actualisation").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(ActualisationRequest.create().withApplicant("f47ac10b-58cc-4372-a567-0e02b2c3d479").withApplicationMonth("2026-13"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void prefill_invalidPartyId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/prefill").queryParam("partyId", "not-a-uuid").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(prefillServiceMock);
	}

	@Test
	void listActualisations_invalidPartyId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/actualisations").queryParam("partyId", "not-a-uuid").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void listActualisations_invalidMunicipalityId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/actualisations").queryParam("partyId", randomUUID().toString()).build(Map.of("municipalityId", "x", "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void archiveToActualisation_invalidMunicipalityId() {
		final var builder = new MultipartBodyBuilder();
		builder.part("file", "%PDF-1.4".getBytes()).filename("tillaggsansokan.pdf");

		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/actualisations/{actualisationId}/archive").queryParam("partyId", "f47ac10b-58cc-4372-a567-0e02b2c3d479").build(Map.of("municipalityId", "x", "namespace", NAMESPACE, "actualisationId", 5012)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void archiveToActualisation_missingFile() {
		final var builder = new MultipartBodyBuilder();
		builder.part("request", se.sundsvall.caremanagement.types.financialassistance.api.model.ArchiveActualisationRequest.create().withTitle("x"), APPLICATION_JSON);

		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/actualisations/{actualisationId}/archive").queryParam("partyId", "f47ac10b-58cc-4372-a567-0e02b2c3d479").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "actualisationId", 5012)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void archiveToActualisation_invalidErrandId() {
		final var builder = new MultipartBodyBuilder();
		builder.part("file", "%PDF-1.4".getBytes()).filename("tillaggsansokan.pdf");
		builder.part("request", se.sundsvall.caremanagement.types.financialassistance.api.model.ArchiveActualisationRequest.create().withErrandId("not-a-uuid"), APPLICATION_JSON);

		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/actualisations/{actualisationId}/archive").queryParam("partyId", "f47ac10b-58cc-4372-a567-0e02b2c3d479").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "actualisationId", 5012)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void archiveToActualisation_invalidPartyId() {
		final var builder = new MultipartBodyBuilder();
		builder.part("file", "%PDF-1.4".getBytes()).filename("tillaggsansokan.pdf");

		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/actualisations/{actualisationId}/archive").queryParam("partyId", "not-a-uuid").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "actualisationId", 5012)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

}
