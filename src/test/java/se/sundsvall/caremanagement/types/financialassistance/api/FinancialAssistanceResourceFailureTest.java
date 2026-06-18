package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ActualisationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateFinancialAssistanceRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.EligibilityRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceData;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormberakningRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApprovalRequest;
import se.sundsvall.caremanagement.types.financialassistance.service.EligibilityService;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceService;
import se.sundsvall.caremanagement.types.financialassistance.service.RenewalPrefillService;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAssistanceResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String CREATE_PATH = "/{municipalityId}/{namespace}/errands/financial-assistance-new";
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance";

	@MockitoBean
	private FinancialAssistanceService serviceMock;

	@MockitoBean
	private EligibilityService eligibilityServiceMock;

	@MockitoBean
	private RenewalPrefillService prefillServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createErrand_blankTitle() {
		final var builder = new MultipartBodyBuilder();
		builder.part("request", CreateFinancialAssistanceRequest.create().withTitle(" ").withData(FinancialAssistanceData.create()), APPLICATION_JSON);

		webTestClient.post()
			.uri(uri -> uri.path(CREATE_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createErrand_missingData() {
		final var builder = new MultipartBodyBuilder();
		builder.part("request", CreateFinancialAssistanceRequest.create().withTitle("ok"), APPLICATION_JSON);

		webTestClient.post()
			.uri(uri -> uri.path(CREATE_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createErrand_invalidApplicationType() {
		final var builder = new MultipartBodyBuilder();
		builder.part("request", CreateFinancialAssistanceRequest.create().withTitle("ok").withData(FinancialAssistanceData.create().withApplicationType("BOGUS")), APPLICATION_JSON);

		webTestClient.post()
			.uri(uri -> uri.path(CREATE_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

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
	void prepareNormberakning_invalidApplicant() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/normberakning/prepare").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(NormberakningRequest.create().withApplicant("123").withApplicationMonth("2026-06"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void commitNormberakning_invalidMonth() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/normberakning/commit").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(NormberakningRequest.create().withApplicant("f47ac10b-58cc-4372-a567-0e02b2c3d479").withApplicationMonth("2026-13"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
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
	void setSectionApproval_missingApproved() {
		webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/errand-1/sections/CALCULATION/approval").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.contentType(APPLICATION_JSON)
			.bodyValue(SectionApprovalRequest.create().withApprovedBy("jane02doe")) // approved is required
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void setSectionApproval_invalidMunicipalityId() {
		webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/errand-1/sections/CALCULATION/approval").build(Map.of("municipalityId", "x", "namespace", NAMESPACE)))
			.contentType(APPLICATION_JSON)
			.bodyValue(SectionApprovalRequest.create().withApproved(true).withApprovedBy("jane02doe"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readErrand_invalidErrandId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{errandId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}
}
