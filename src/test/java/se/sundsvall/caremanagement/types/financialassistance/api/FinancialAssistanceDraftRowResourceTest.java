package se.sundsvall.caremanagement.types.financialassistance.api;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonRow;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceDraftRowService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAssistanceDraftRowResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String ROW_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance";

	@MockitoBean
	private FinancialAssistanceDraftRowService draftRowServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void addDraftIncome() {
		when(draftRowServiceMock.addDraftIncome(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(NormIncomeInput.class)))
			.thenReturn(NormIncomeRow.create().withId("r1").withOrigin("CASEWORKER"));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/" + ERRAND_ID + "/calculation/draft/incomes").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(new NormIncomeInput().withTypeId(20).withApplicantCaseworkerAmount(new BigDecimal("3000")))
			.exchange()
			.expectStatus().isOk()
			.expectBody(NormIncomeRow.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getId()).isEqualTo("r1");
		verify(draftRowServiceMock).addDraftIncome(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(NormIncomeInput.class));
	}

	@Test
	void patchDraftPerson() {
		when(draftRowServiceMock.patchDraftPerson(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(ROW_ID), any(NormPersonInput.class)))
			.thenReturn(NormPersonRow.create().withId(ROW_ID).withCaseworkerDays(15));

		final var response = webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/" + ERRAND_ID + "/calculation/draft/persons/" + ROW_ID).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(new NormPersonInput().withCaseworkerDays(15))
			.exchange()
			.expectStatus().isOk()
			.expectBody(NormPersonRow.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getCaseworkerDays()).isEqualTo(15);
		verify(draftRowServiceMock).patchDraftPerson(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(ROW_ID), any(NormPersonInput.class));
	}

	@Test
	void deleteDraftExpense() {
		when(draftRowServiceMock.setDraftExpenseDeleted(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ROW_ID, true))
			.thenReturn(NormExpenseRow.create().withId(ROW_ID).withDeleted(true));

		final var response = webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/" + ERRAND_ID + "/calculation/draft/expenses/" + ROW_ID).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(NormExpenseRow.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.isDeleted()).isTrue();
		verify(draftRowServiceMock).setDraftExpenseDeleted(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ROW_ID, true);
	}

	@Test
	void incomeRowRemainingEndpoints() {
		when(draftRowServiceMock.patchDraftIncome(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(ROW_ID), any(NormIncomeInput.class))).thenReturn(NormIncomeRow.create().withId(ROW_ID));
		when(draftRowServiceMock.setDraftIncomeDeleted(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ROW_ID, true)).thenReturn(NormIncomeRow.create().withId(ROW_ID).withDeleted(true));
		when(draftRowServiceMock.setDraftIncomeDeleted(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ROW_ID, false)).thenReturn(NormIncomeRow.create().withId(ROW_ID).withDeleted(false));

		webTestClient.patch().uri(uri -> uri.path(PATH + "/" + ERRAND_ID + "/calculation/draft/incomes/" + ROW_ID).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(new NormIncomeInput().withApplicantCaseworkerAmount(new BigDecimal("1000"))).exchange().expectStatus().isOk();
		webTestClient.delete().uri(uri -> uri.path(PATH + "/" + ERRAND_ID + "/calculation/draft/incomes/" + ROW_ID).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE))).exchange().expectStatus().isOk();
		webTestClient.post().uri(uri -> uri.path(PATH + "/" + ERRAND_ID + "/calculation/draft/incomes/" + ROW_ID + "/restore").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE))).exchange().expectStatus().isOk();

		verify(draftRowServiceMock).patchDraftIncome(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(ROW_ID), any(NormIncomeInput.class));
		verify(draftRowServiceMock).setDraftIncomeDeleted(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ROW_ID, true);
		verify(draftRowServiceMock).setDraftIncomeDeleted(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ROW_ID, false);
	}

	@Test
	void expenseRowRemainingEndpoints() {
		when(draftRowServiceMock.addDraftExpense(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(NormExpenseInput.class))).thenReturn(NormExpenseRow.create().withId(ROW_ID));
		when(draftRowServiceMock.patchDraftExpense(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(ROW_ID), any(NormExpenseInput.class))).thenReturn(NormExpenseRow.create().withId(ROW_ID));
		when(draftRowServiceMock.setDraftExpenseDeleted(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ROW_ID, false)).thenReturn(NormExpenseRow.create().withId(ROW_ID));

		webTestClient.post().uri(uri -> uri.path(PATH + "/" + ERRAND_ID + "/calculation/draft/expenses").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(new NormExpenseInput().withCostType("RENT")).exchange().expectStatus().isOk();
		webTestClient.patch().uri(uri -> uri.path(PATH + "/" + ERRAND_ID + "/calculation/draft/expenses/" + ROW_ID).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(new NormExpenseInput().withCaseworkerAmount(new BigDecimal("8000"))).exchange().expectStatus().isOk();
		webTestClient.post().uri(uri -> uri.path(PATH + "/" + ERRAND_ID + "/calculation/draft/expenses/" + ROW_ID + "/restore").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE))).exchange().expectStatus().isOk();

		verify(draftRowServiceMock).addDraftExpense(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(NormExpenseInput.class));
		verify(draftRowServiceMock).patchDraftExpense(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(ROW_ID), any(NormExpenseInput.class));
		verify(draftRowServiceMock).setDraftExpenseDeleted(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ROW_ID, false);
	}

	@Test
	void personRowRemainingEndpoints() {
		when(draftRowServiceMock.addDraftPerson(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(NormPersonInput.class))).thenReturn(NormPersonRow.create().withId(ROW_ID));
		when(draftRowServiceMock.setDraftPersonDeleted(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ROW_ID, true)).thenReturn(NormPersonRow.create().withId(ROW_ID).withDeleted(true));
		when(draftRowServiceMock.setDraftPersonDeleted(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ROW_ID, false)).thenReturn(NormPersonRow.create().withId(ROW_ID));

		webTestClient.post().uri(uri -> uri.path(PATH + "/" + ERRAND_ID + "/calculation/draft/persons").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(new NormPersonInput().withPartyId("party-1").withRole("CHILD")).exchange().expectStatus().isOk();
		webTestClient.delete().uri(uri -> uri.path(PATH + "/" + ERRAND_ID + "/calculation/draft/persons/" + ROW_ID).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE))).exchange().expectStatus().isOk();
		webTestClient.post().uri(uri -> uri.path(PATH + "/" + ERRAND_ID + "/calculation/draft/persons/" + ROW_ID + "/restore").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE))).exchange().expectStatus().isOk();

		verify(draftRowServiceMock).addDraftPerson(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(NormPersonInput.class));
		verify(draftRowServiceMock).setDraftPersonDeleted(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ROW_ID, true);
		verify(draftRowServiceMock).setDraftPersonDeleted(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ROW_ID, false);
	}

}
