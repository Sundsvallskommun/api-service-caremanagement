package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.List;
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
import se.sundsvall.caremanagement.types.financialassistance.api.model.ActualisationResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateFinancialAssistanceRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.EligibilityRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.EligibilityResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceData;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormberakningRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormberakningResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.RenewalPrefill;
import se.sundsvall.caremanagement.types.financialassistance.service.EligibilityService;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceService;
import se.sundsvall.caremanagement.types.financialassistance.service.RenewalPrefillService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAssistanceResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String SLUG = "financial-assistance-new";
	private static final String CREATE_PATH = "/{municipalityId}/{namespace}/errands/" + SLUG;
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance";

	@MockitoBean
	private FinancialAssistanceService serviceMock;

	@MockitoBean
	private EligibilityService eligibilityServiceMock;

	@MockitoBean
	private RenewalPrefillService prefillServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	private Map<String, ?> base() {
		return Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE);
	}

	@Test
	void createErrand() {
		when(serviceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class), any())).thenReturn(ERRAND_ID);

		final var builder = new MultipartBodyBuilder();
		builder.part("request", CreateFinancialAssistanceRequest.create().withTitle("Min ansökan").withData(FinancialAssistanceData.create()), APPLICATION_JSON);

		webTestClient.post()
			.uri(uri -> uri.path(CREATE_PATH).build(base()))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isCreated();

		verify(serviceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class), any());
	}

	@Test
	void createErrandWithAttachments() {
		when(serviceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class), any())).thenReturn(ERRAND_ID);

		final var builder = new MultipartBodyBuilder();
		builder.part("request", CreateFinancialAssistanceRequest.create().withTitle("Med bilagor").withData(FinancialAssistanceData.create()), APPLICATION_JSON);
		builder.part("attachments", "hyreskontrakt".getBytes()).filename("hyreskontrakt.pdf");
		builder.part("attachments", "hyresavi".getBytes()).filename("hyresavi.png");

		webTestClient.post()
			.uri(uri -> uri.path(CREATE_PATH).build(base()))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isCreated();

		verify(serviceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class), any());
	}

	@Test
	void readErrand() {
		when(serviceMock.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(FinancialAssistanceView.create().withId(ERRAND_ID));

		final var view = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{errandId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(FinancialAssistanceView.class)
			.returnResult()
			.getResponseBody();

		assertThat(view).isNotNull();
		assertThat(view.getId()).isEqualTo(ERRAND_ID);
		verify(serviceMock).read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void checkEligibility() {
		when(eligibilityServiceMock.evaluate(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(EligibilityRequest.class)))
			.thenReturn(EligibilityResponse.create().withReasonCode("EXISTING_CASE"));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/eligibility").build(base()))
			.bodyValue(EligibilityRequest.create().withApplicant("f47ac10b-58cc-4372-a567-0e02b2c3d479"))
			.exchange()
			.expectStatus().isOk()
			.expectBody(EligibilityResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getReasonCode()).isEqualTo("EXISTING_CASE");
		verify(eligibilityServiceMock).evaluate(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(EligibilityRequest.class));
	}

	@Test
	void prepareNormberakning() {
		when(serviceMock.prepareNormberakning(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(NormberakningRequest.class)))
			.thenReturn(NormberakningResponse.create().withInformationComplete(false).withMissingIncomeTypes(List.of("Dagersättning")));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/normberakning/prepare").build(base()))
			.bodyValue(NormberakningRequest.create().withApplicant("f47ac10b-58cc-4372-a567-0e02b2c3d479").withApplicationMonth("2026-06"))
			.exchange()
			.expectStatus().isOk()
			.expectBody(NormberakningResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.isInformationComplete()).isFalse();
		assertThat(response.getMissingIncomeTypes()).containsExactly("Dagersättning");
		verify(serviceMock).prepareNormberakning(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(NormberakningRequest.class));
	}

	@Test
	void commitNormberakning() {
		when(serviceMock.commitNormberakning(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(NormberakningRequest.class)))
			.thenReturn(NormberakningResponse.create().withCalculationId(4711));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/normberakning/commit").build(base()))
			.bodyValue(NormberakningRequest.create().withApplicant("f47ac10b-58cc-4372-a567-0e02b2c3d479").withApplicationMonth("2026-06"))
			.exchange()
			.expectStatus().isOk()
			.expectBody(NormberakningResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getCalculationId()).isEqualTo(4711);
		verify(serviceMock).commitNormberakning(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(NormberakningRequest.class));
	}

	@Test
	void createActualisation() {
		when(serviceMock.createActualisation(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(ActualisationRequest.class)))
			.thenReturn(ActualisationResponse.create().withActualisationId(5012));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/actualisation").build(base()))
			.bodyValue(ActualisationRequest.create().withApplicant("f47ac10b-58cc-4372-a567-0e02b2c3d479").withApplicationMonth("2026-06"))
			.exchange()
			.expectStatus().isOk()
			.expectBody(ActualisationResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getActualisationId()).isEqualTo(5012);
		verify(serviceMock).createActualisation(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(ActualisationRequest.class));
	}

	@Test
	void checkPaymentStatus() {
		when(serviceMock.checkPaymentStatus(eq(MUNICIPALITY_ID), any(PaymentStatusRequest.class)))
			.thenReturn(PaymentStatusResponse.create().withEffectuated(true).withPaymentDate("2026-05-27"));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/payment-status").build(base()))
			.bodyValue(PaymentStatusRequest.create().withApplicant("f47ac10b-58cc-4372-a567-0e02b2c3d479").withApplicationMonth("2026-06"))
			.exchange()
			.expectStatus().isOk()
			.expectBody(PaymentStatusResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getEffectuated()).isTrue();
		assertThat(response.getPaymentDate()).isEqualTo("2026-05-27");
		verify(serviceMock).checkPaymentStatus(eq(MUNICIPALITY_ID), any(PaymentStatusRequest.class));
	}

	@Test
	void prefill() {
		final var partyId = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
		when(prefillServiceMock.prefill(MUNICIPALITY_ID, partyId)).thenReturn(RenewalPrefill.create().withLifecareChecked(true));

		final var prefill = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/prefill").queryParam("partyId", partyId).build(base()))
			.exchange()
			.expectStatus().isOk()
			.expectBody(RenewalPrefill.class)
			.returnResult()
			.getResponseBody();

		assertThat(prefill).isNotNull();
		assertThat(prefill.isLifecareChecked()).isTrue();
		verify(prefillServiceMock).prefill(MUNICIPALITY_ID, partyId);
	}

	@Test
	void updateData() {
		webTestClient.put()
			.uri(uri -> uri.path(PATH + "/{errandId}/data").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(FinancialAssistanceData.create().withApplicationType("NEW"))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).updateData(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(FinancialAssistanceData.class));
	}
}
