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
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance";

	@MockitoBean
	private FinancialAssistanceDraftRowService draftRowServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void addDraftIncome() {
		when(draftRowServiceMock.addDraftIncome(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), any(NormIncomeInput.class)))
			.thenReturn(NormIncomeRow.create().withId("r1").withOrigin("CASEWORKER"));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/incomes").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(new NormIncomeInput().withTypeId(20).withApplicantCaseworkerAmount(new BigDecimal("3000")))
			.exchange()
			.expectStatus().isOk()
			.expectBody(NormIncomeRow.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getId()).isEqualTo("r1");
		verify(draftRowServiceMock).addDraftIncome(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), any(NormIncomeInput.class));
	}

	@Test
	void patchDraftPerson() {
		when(draftRowServiceMock.patchDraftPerson(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), eq("r2"), any(NormPersonInput.class)))
			.thenReturn(NormPersonRow.create().withId("r2").withCaseworkerDays(15));

		final var response = webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/persons/r2").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(new NormPersonInput().withCaseworkerDays(15))
			.exchange()
			.expectStatus().isOk()
			.expectBody(NormPersonRow.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getCaseworkerDays()).isEqualTo(15);
		verify(draftRowServiceMock).patchDraftPerson(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), eq("r2"), any(NormPersonInput.class));
	}

	@Test
	void deleteDraftExpense() {
		when(draftRowServiceMock.setDraftExpenseDeleted(MUNICIPALITY_ID, NAMESPACE, "errand-1", "r9", true))
			.thenReturn(NormExpenseRow.create().withId("r9").withDeleted(true));

		final var response = webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/expenses/r9").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(NormExpenseRow.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.isDeleted()).isTrue();
		verify(draftRowServiceMock).setDraftExpenseDeleted(MUNICIPALITY_ID, NAMESPACE, "errand-1", "r9", true);
	}

	@Test
	void incomeRowRemainingEndpoints() {
		when(draftRowServiceMock.patchDraftIncome(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), eq("r1"), any(NormIncomeInput.class))).thenReturn(NormIncomeRow.create().withId("r1"));
		when(draftRowServiceMock.setDraftIncomeDeleted(MUNICIPALITY_ID, NAMESPACE, "errand-1", "r1", true)).thenReturn(NormIncomeRow.create().withId("r1").withDeleted(true));
		when(draftRowServiceMock.setDraftIncomeDeleted(MUNICIPALITY_ID, NAMESPACE, "errand-1", "r1", false)).thenReturn(NormIncomeRow.create().withId("r1").withDeleted(false));

		webTestClient.patch().uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/incomes/r1").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(new NormIncomeInput().withApplicantCaseworkerAmount(new BigDecimal("1000"))).exchange().expectStatus().isOk();
		webTestClient.delete().uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/incomes/r1").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE))).exchange().expectStatus().isOk();
		webTestClient.post().uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/incomes/r1/restore").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE))).exchange().expectStatus().isOk();

		verify(draftRowServiceMock).patchDraftIncome(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), eq("r1"), any(NormIncomeInput.class));
		verify(draftRowServiceMock).setDraftIncomeDeleted(MUNICIPALITY_ID, NAMESPACE, "errand-1", "r1", true);
		verify(draftRowServiceMock).setDraftIncomeDeleted(MUNICIPALITY_ID, NAMESPACE, "errand-1", "r1", false);
	}

	@Test
	void expenseRowRemainingEndpoints() {
		when(draftRowServiceMock.addDraftExpense(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), any(NormExpenseInput.class))).thenReturn(NormExpenseRow.create().withId("e1"));
		when(draftRowServiceMock.patchDraftExpense(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), eq("e1"), any(NormExpenseInput.class))).thenReturn(NormExpenseRow.create().withId("e1"));
		when(draftRowServiceMock.setDraftExpenseDeleted(MUNICIPALITY_ID, NAMESPACE, "errand-1", "e1", false)).thenReturn(NormExpenseRow.create().withId("e1"));

		webTestClient.post().uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/expenses").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(new NormExpenseInput().withCostType("RENT")).exchange().expectStatus().isOk();
		webTestClient.patch().uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/expenses/e1").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(new NormExpenseInput().withCaseworkerAmount(new BigDecimal("8000"))).exchange().expectStatus().isOk();
		webTestClient.post().uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/expenses/e1/restore").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE))).exchange().expectStatus().isOk();

		verify(draftRowServiceMock).addDraftExpense(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), any(NormExpenseInput.class));
		verify(draftRowServiceMock).patchDraftExpense(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), eq("e1"), any(NormExpenseInput.class));
		verify(draftRowServiceMock).setDraftExpenseDeleted(MUNICIPALITY_ID, NAMESPACE, "errand-1", "e1", false);
	}

	@Test
	void personRowRemainingEndpoints() {
		when(draftRowServiceMock.addDraftPerson(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), any(NormPersonInput.class))).thenReturn(NormPersonRow.create().withId("p1"));
		when(draftRowServiceMock.setDraftPersonDeleted(MUNICIPALITY_ID, NAMESPACE, "errand-1", "p1", true)).thenReturn(NormPersonRow.create().withId("p1").withDeleted(true));
		when(draftRowServiceMock.setDraftPersonDeleted(MUNICIPALITY_ID, NAMESPACE, "errand-1", "p1", false)).thenReturn(NormPersonRow.create().withId("p1"));

		webTestClient.post().uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/persons").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(new NormPersonInput().withPartyId("party-1").withRole("CHILD")).exchange().expectStatus().isOk();
		webTestClient.delete().uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/persons/p1").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE))).exchange().expectStatus().isOk();
		webTestClient.post().uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/persons/p1/restore").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE))).exchange().expectStatus().isOk();

		verify(draftRowServiceMock).addDraftPerson(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), any(NormPersonInput.class));
		verify(draftRowServiceMock).setDraftPersonDeleted(MUNICIPALITY_ID, NAMESPACE, "errand-1", "p1", true);
		verify(draftRowServiceMock).setDraftPersonDeleted(MUNICIPALITY_ID, NAMESPACE, "errand-1", "p1", false);
	}

}
