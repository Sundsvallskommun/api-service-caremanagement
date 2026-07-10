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
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormHeaderInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeRow;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceCalculationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAssistanceCalculationResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance";

	@MockitoBean
	private FinancialAssistanceCalculationService calculationServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void prepareCalculation() {
		when(calculationServiceMock.prepareCalculation(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(CalculationRequest.class)))
			.thenReturn(CalculationResponse.create().withInformationComplete(false).withMissingIncomeTypes(List.of("Dagersättning")));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/calculation/prepare").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(CalculationRequest.create().withApplicant("f47ac10b-58cc-4372-a567-0e02b2c3d479").withApplicationMonth("2026-06").withErrandId("cb20c51f-fcf3-42c0-b613-de563634a8ec"))
			.exchange()
			.expectStatus().isOk()
			.expectBody(CalculationResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.isInformationComplete()).isFalse();
		assertThat(response.getMissingIncomeTypes()).containsExactly("Dagersättning");
		verify(calculationServiceMock).prepareCalculation(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(CalculationRequest.class));
	}

	@Test
	void getDraft() {
		when(calculationServiceMock.getDraft(MUNICIPALITY_ID, NAMESPACE, "errand-1"))
			.thenReturn(CalculationDraft.create().withErrandId("errand-1")
				.withIncomes(List.of(NormIncomeRow.create().withTypeName("Bostadsbidrag").withApplicantProcessAmount(new BigDecimal("1850")))));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/errand-1/calculation/draft").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(CalculationDraft.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getIncomes()).hasSize(1);
		assertThat(response.getIncomes().getFirst().getTypeName()).isEqualTo("Bostadsbidrag");
		verify(calculationServiceMock).getDraft(MUNICIPALITY_ID, NAMESPACE, "errand-1");
	}

	@Test
	void patchDraftHeader() {
		when(calculationServiceMock.patchDraftHeader(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), any(NormHeaderInput.class)))
			.thenReturn(CalculationDraft.create().withNormId(5).withHouseholdSize(1));

		final var response = webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/header").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(new NormHeaderInput().withNormId(5).withHasCustomHouseholdSize(true).withHouseholdSize(1))
			.exchange()
			.expectStatus().isOk()
			.expectBody(CalculationDraft.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getNormId()).isEqualTo(5);
		verify(calculationServiceMock).patchDraftHeader(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), any(NormHeaderInput.class));
	}

	@Test
	void commitCalculation() {
		when(calculationServiceMock.commitCalculation(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(CalculationRequest.class)))
			.thenReturn(CalculationResponse.create().withCalculationId(4711));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/calculation/commit").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(CalculationRequest.create().withApplicant("f47ac10b-58cc-4372-a567-0e02b2c3d479").withApplicationMonth("2026-06").withErrandId("cb20c51f-fcf3-42c0-b613-de563634a8ec"))
			.exchange()
			.expectStatus().isOk()
			.expectBody(CalculationResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getCalculationId()).isEqualTo(4711);
		verify(calculationServiceMock).commitCalculation(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(CalculationRequest.class));
	}

	@Test
	void commitFromApplication() {
		when(calculationServiceMock.commitFromApplication(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(CalculationRequest.class)))
			.thenReturn(CalculationResponse.create().withCalculationId(5001));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/calculation/from-application").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(CalculationRequest.create().withApplicant("f47ac10b-58cc-4372-a567-0e02b2c3d479").withApplicationMonth("2026-06").withErrandId("cb20c51f-fcf3-42c0-b613-de563634a8ec"))
			.exchange()
			.expectStatus().isOk()
			.expectBody(CalculationResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getCalculationId()).isEqualTo(5001);
		verify(calculationServiceMock).commitFromApplication(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(CalculationRequest.class));
	}

}
