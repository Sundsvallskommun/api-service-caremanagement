package se.sundsvall.caremanagement.types.financialassistance.api;

import java.math.BigDecimal;
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
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateFinancialAssistanceRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.EligibilityRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.EligibilityResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceData;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormHeaderInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.RenewalPrefill;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApproval;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApprovalRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApprovals;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Warning;
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
		builder.part("request", CreateFinancialAssistanceRequest.create().withTitle("Min application").withData(FinancialAssistanceData.create()), APPLICATION_JSON);

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
	void prepareCalculation() {
		when(serviceMock.prepareCalculation(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(CalculationRequest.class)))
			.thenReturn(CalculationResponse.create().withInformationComplete(false).withMissingIncomeTypes(List.of("Dagersättning")));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/calculation/prepare").build(base()))
			.bodyValue(CalculationRequest.create().withApplicant("f47ac10b-58cc-4372-a567-0e02b2c3d479").withApplicationMonth("2026-06"))
			.exchange()
			.expectStatus().isOk()
			.expectBody(CalculationResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.isInformationComplete()).isFalse();
		assertThat(response.getMissingIncomeTypes()).containsExactly("Dagersättning");
		verify(serviceMock).prepareCalculation(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(CalculationRequest.class));
	}

	@Test
	void listWarnings() {
		when(serviceMock.listWarnings(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1")))
			.thenReturn(List.of(Warning.create().withId("w1").withType("MISSING_SSBTEK").withStatus("OPEN")));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/errand-1/warnings").build(base()))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(Warning.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).hasSize(1);
		assertThat(response.getFirst().getType()).isEqualTo("MISSING_SSBTEK");
		verify(serviceMock).listWarnings(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"));
	}

	@Test
	void updateWarning() {
		when(serviceMock.updateWarning(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), eq("w1"), eq("CLOSED")))
			.thenReturn(Warning.create().withId("w1").withStatus("CLOSED"));

		final var response = webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/errand-1/warnings/w1").queryParam("status", "CLOSED").build(base()))
			.exchange()
			.expectStatus().isOk()
			.expectBody(Warning.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo("CLOSED");
		verify(serviceMock).updateWarning(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), eq("w1"), eq("CLOSED"));
	}

	@Test
	void getSectionApprovals() {
		final var approvals = SectionApprovals.create()
			.withCalculation(SectionApproval.create().withSection("CALCULATION").withApproved(true))
			.withPayment(SectionApproval.create().withSection("PAYMENT").withApproved(false))
			.withDecision(SectionApproval.create().withSection("DECISION").withApproved(false));
		when(serviceMock.getSectionApprovals(MUNICIPALITY_ID, NAMESPACE, "errand-1")).thenReturn(approvals);

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/errand-1/sections/approvals").build(base()))
			.exchange()
			.expectStatus().isOk()
			.expectBody(SectionApprovals.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getCalculation().isApproved()).isTrue();
		assertThat(response.getPayment().isApproved()).isFalse();
		verify(serviceMock).getSectionApprovals(MUNICIPALITY_ID, NAMESPACE, "errand-1");
	}

	@Test
	void setSectionApproval() {
		when(serviceMock.setSectionApproval(MUNICIPALITY_ID, NAMESPACE, "errand-1", "CALCULATION", true, "jane02doe"))
			.thenReturn(SectionApproval.create().withSection("CALCULATION").withApproved(true).withApprovedBy("jane02doe"));

		final var response = webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/errand-1/sections/CALCULATION/approval").build(base()))
			.bodyValue(SectionApprovalRequest.create().withApproved(true).withApprovedBy("jane02doe"))
			.exchange()
			.expectStatus().isOk()
			.expectBody(SectionApproval.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getSection()).isEqualTo("CALCULATION");
		assertThat(response.isApproved()).isTrue();
		assertThat(response.getApprovedBy()).isEqualTo("jane02doe");
		verify(serviceMock).setSectionApproval(MUNICIPALITY_ID, NAMESPACE, "errand-1", "CALCULATION", true, "jane02doe");
	}

	@Test
	void getDraft() {
		when(serviceMock.getDraft(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1")))
			.thenReturn(CalculationDraft.create().withErrandId("errand-1")
				.withIncomes(List.of(NormIncomeRow.create().withTypeName("Bostadsbidrag").withApplicantProcessAmount(new BigDecimal("1850")))));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/errand-1/calculation/draft").build(base()))
			.exchange()
			.expectStatus().isOk()
			.expectBody(CalculationDraft.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getIncomes()).hasSize(1);
		assertThat(response.getIncomes().getFirst().getTypeName()).isEqualTo("Bostadsbidrag");
		verify(serviceMock).getDraft(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"));
	}

	@Test
	void patchDraftHeader() {
		when(serviceMock.patchDraftHeader(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), any(NormHeaderInput.class)))
			.thenReturn(CalculationDraft.create().withNormId(5).withHouseholdSize(1));

		final var response = webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/header").build(base()))
			.bodyValue(new NormHeaderInput().withNormId(5).withHasCustomHouseholdSize(true).withHouseholdSize(1))
			.exchange()
			.expectStatus().isOk()
			.expectBody(CalculationDraft.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getNormId()).isEqualTo(5);
		verify(serviceMock).patchDraftHeader(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), any(NormHeaderInput.class));
	}

	@Test
	void addDraftIncome() {
		when(serviceMock.addDraftIncome(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), any(NormIncomeInput.class)))
			.thenReturn(NormIncomeRow.create().withId("r1").withOrigin("CASEWORKER"));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/incomes").build(base()))
			.bodyValue(new NormIncomeInput().withTypeId(20).withApplicantCaseworkerAmount(new BigDecimal("3000")))
			.exchange()
			.expectStatus().isOk()
			.expectBody(NormIncomeRow.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getId()).isEqualTo("r1");
		verify(serviceMock).addDraftIncome(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), any(NormIncomeInput.class));
	}

	@Test
	void patchDraftPerson() {
		when(serviceMock.patchDraftPerson(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), eq("r2"), any(NormPersonInput.class)))
			.thenReturn(NormPersonRow.create().withId("r2").withCaseworkerDays(15));

		final var response = webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/persons/r2").build(base()))
			.bodyValue(new NormPersonInput().withCaseworkerDays(15))
			.exchange()
			.expectStatus().isOk()
			.expectBody(NormPersonRow.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getCaseworkerDays()).isEqualTo(15);
		verify(serviceMock).patchDraftPerson(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), eq("r2"), any(NormPersonInput.class));
	}

	@Test
	void deleteDraftExpense() {
		when(serviceMock.setDraftExpenseDeleted(MUNICIPALITY_ID, NAMESPACE, "errand-1", "r9", true))
			.thenReturn(NormExpenseRow.create().withId("r9").withDeleted(true));

		final var response = webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/expenses/r9").build(base()))
			.exchange()
			.expectStatus().isOk()
			.expectBody(NormExpenseRow.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.isDeleted()).isTrue();
		verify(serviceMock).setDraftExpenseDeleted(MUNICIPALITY_ID, NAMESPACE, "errand-1", "r9", true);
	}

	@Test
	void incomeRowRemainingEndpoints() {
		when(serviceMock.patchDraftIncome(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), eq("r1"), any(NormIncomeInput.class))).thenReturn(NormIncomeRow.create().withId("r1"));
		when(serviceMock.setDraftIncomeDeleted(MUNICIPALITY_ID, NAMESPACE, "errand-1", "r1", true)).thenReturn(NormIncomeRow.create().withId("r1").withDeleted(true));
		when(serviceMock.setDraftIncomeDeleted(MUNICIPALITY_ID, NAMESPACE, "errand-1", "r1", false)).thenReturn(NormIncomeRow.create().withId("r1").withDeleted(false));

		webTestClient.patch().uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/incomes/r1").build(base()))
			.bodyValue(new NormIncomeInput().withApplicantCaseworkerAmount(new BigDecimal("1000"))).exchange().expectStatus().isOk();
		webTestClient.delete().uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/incomes/r1").build(base())).exchange().expectStatus().isOk();
		webTestClient.post().uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/incomes/r1/restore").build(base())).exchange().expectStatus().isOk();

		verify(serviceMock).patchDraftIncome(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), eq("r1"), any(NormIncomeInput.class));
		verify(serviceMock).setDraftIncomeDeleted(MUNICIPALITY_ID, NAMESPACE, "errand-1", "r1", true);
		verify(serviceMock).setDraftIncomeDeleted(MUNICIPALITY_ID, NAMESPACE, "errand-1", "r1", false);
	}

	@Test
	void expenseRowRemainingEndpoints() {
		when(serviceMock.addDraftExpense(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), any(NormExpenseInput.class))).thenReturn(NormExpenseRow.create().withId("e1"));
		when(serviceMock.patchDraftExpense(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), eq("e1"), any(NormExpenseInput.class))).thenReturn(NormExpenseRow.create().withId("e1"));
		when(serviceMock.setDraftExpenseDeleted(MUNICIPALITY_ID, NAMESPACE, "errand-1", "e1", false)).thenReturn(NormExpenseRow.create().withId("e1"));

		webTestClient.post().uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/expenses").build(base()))
			.bodyValue(new NormExpenseInput().withCostType("RENT")).exchange().expectStatus().isOk();
		webTestClient.patch().uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/expenses/e1").build(base()))
			.bodyValue(new NormExpenseInput().withCaseworkerAmount(new BigDecimal("8000"))).exchange().expectStatus().isOk();
		webTestClient.post().uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/expenses/e1/restore").build(base())).exchange().expectStatus().isOk();

		verify(serviceMock).addDraftExpense(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), any(NormExpenseInput.class));
		verify(serviceMock).patchDraftExpense(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), eq("e1"), any(NormExpenseInput.class));
		verify(serviceMock).setDraftExpenseDeleted(MUNICIPALITY_ID, NAMESPACE, "errand-1", "e1", false);
	}

	@Test
	void personRowRemainingEndpoints() {
		when(serviceMock.addDraftPerson(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), any(NormPersonInput.class))).thenReturn(NormPersonRow.create().withId("p1"));
		when(serviceMock.setDraftPersonDeleted(MUNICIPALITY_ID, NAMESPACE, "errand-1", "p1", true)).thenReturn(NormPersonRow.create().withId("p1").withDeleted(true));
		when(serviceMock.setDraftPersonDeleted(MUNICIPALITY_ID, NAMESPACE, "errand-1", "p1", false)).thenReturn(NormPersonRow.create().withId("p1"));

		webTestClient.post().uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/persons").build(base()))
			.bodyValue(new NormPersonInput().withPartyId("party-1").withRole("CHILD")).exchange().expectStatus().isOk();
		webTestClient.delete().uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/persons/p1").build(base())).exchange().expectStatus().isOk();
		webTestClient.post().uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/persons/p1/restore").build(base())).exchange().expectStatus().isOk();

		verify(serviceMock).addDraftPerson(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), any(NormPersonInput.class));
		verify(serviceMock).setDraftPersonDeleted(MUNICIPALITY_ID, NAMESPACE, "errand-1", "p1", true);
		verify(serviceMock).setDraftPersonDeleted(MUNICIPALITY_ID, NAMESPACE, "errand-1", "p1", false);
	}

	@Test
	void commitCalculation() {
		when(serviceMock.commitCalculation(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(CalculationRequest.class)))
			.thenReturn(CalculationResponse.create().withCalculationId(4711));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/calculation/commit").build(base()))
			.bodyValue(CalculationRequest.create().withApplicant("f47ac10b-58cc-4372-a567-0e02b2c3d479").withApplicationMonth("2026-06"))
			.exchange()
			.expectStatus().isOk()
			.expectBody(CalculationResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getCalculationId()).isEqualTo(4711);
		verify(serviceMock).commitCalculation(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(CalculationRequest.class));
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
