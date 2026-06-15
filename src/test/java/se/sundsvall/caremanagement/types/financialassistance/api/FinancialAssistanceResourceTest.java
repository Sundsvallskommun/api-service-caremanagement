package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateFinancialAssistanceRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.EligibilityRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.EligibilityResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceData;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceView;
import se.sundsvall.caremanagement.types.financialassistance.service.EligibilityService;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

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

	@Autowired
	private WebTestClient webTestClient;

	private Map<String, ?> base() {
		return Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE);
	}

	@Test
	void createErrand() {
		when(serviceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class))).thenReturn(ERRAND_ID);

		webTestClient.post()
			.uri(uri -> uri.path(CREATE_PATH).build(base()))
			.bodyValue(CreateFinancialAssistanceRequest.create().withTitle("Min ansökan").withData(FinancialAssistanceData.create()))
			.exchange()
			.expectStatus().isCreated();

		verify(serviceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class));
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
			.thenReturn(EligibilityResponse.create().withReasonCode("NO_OPEN_CASE"));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/eligibility").build(base()))
			.bodyValue(EligibilityRequest.create().withApplicant("198001012389"))
			.exchange()
			.expectStatus().isOk()
			.expectBody(EligibilityResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getReasonCode()).isEqualTo("NO_OPEN_CASE");
		verify(eligibilityServiceMock).evaluate(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(EligibilityRequest.class));
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
