package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormHeaderInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningRowInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningTypeOption;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningTypes;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceCalculationService;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceDraftRowService;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareAccessRecorder;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareErrand;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareErrandService;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareCalculationEditService.CalculationChange;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.NormberakningDraftReader.DraftWithNumbers;
import se.sundsvall.dept44.problem.Problem;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.draft;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.forEdit;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.json;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.savedCalculation;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.objects;

@ExtendWith(MockitoExtension.class)
class ErrandNormberakningServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "e1";
	private static final String ROW_ID = "cb20c51f-fcf3-42c0-b613-de563634a8ec";
	private static final LifecareErrand UNLINKED = new LifecareErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 1, null, null, null, 2026, 9);
	private static final LifecareErrand LINKED = new LifecareErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 1, 30, null, null, 2026, 9);

	@Mock
	private LifecareErrandService errandService;
	@Mock
	private LifecareCalculationEditService editService;
	@Mock
	private LifecareCalculationClient client;
	@Mock
	private NormberakningDraftReader draftReader;
	@Mock
	private FinancialAssistanceCalculationService calculationService;
	@Mock
	private FinancialAssistanceDraftRowService draftRowService;
	@Mock
	private LifecareAccessRecorder recorder;

	@InjectMocks
	private ErrandNormberakningService service;

	@Captor
	private ArgumentCaptor<CalculationChange> changeCaptor;

	private void errandIs(final LifecareErrand errand) {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errand);
	}

	/** The change the service asked the edit service to make, applied to beräkning 30. */
	private ObjectNode appliedChange() {
		verify(editService).change(eq(LINKED), eq(30), changeCaptor.capture(), eq(false));
		return changeCaptor.getValue().apply(savedCalculation(), forEdit(savedCalculation()));
	}

	private void refusedChange(final String reason) {
		verify(editService).change(eq(LINKED), eq(30), changeCaptor.capture(), eq(false));
		assertThatThrownBy(() -> changeCaptor.getValue().apply(savedCalculation(), forEdit(savedCalculation())))
			.hasFieldOrPropertyWithValue("status", UNPROCESSABLE_CONTENT)
			.hasMessageContaining(reason);
	}

	@Test
	void showsCaremsDraftUntilTheBeräkningIsSavedInLifecare() {
		errandIs(UNLINKED);
		when(draftReader.read(UNLINKED)).thenReturn(new DraftWithNumbers(draft(), Map.of("p1", "880209-T050")));

		final var view = service.readDraft(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(view.getSource()).isEqualTo("CAREM");
		assertThat(view.getNormId()).isEqualTo(1);
		assertThat(view.getPersons().getFirst().getPersonalNumber()).isEqualTo("880209-T050");
		verifyNoInteractions(editService);
	}

	@Test
	void showsLifecaresBeräkningOnceItIsSavedThere() {
		errandIs(LINKED);
		final var lifecareView = NormberakningDraft.create().withSource("LIFECARE");
		when(editService.readDraftView(LINKED, 30)).thenReturn(lifecareView);

		assertThat(service.readDraft(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isSameAs(lifecareView);
		verifyNoInteractions(draftReader);
	}

	@Test
	void offersCaremsTypesAndLifecaresNormsBeforeTheBeräkningIsInLifecare() {
		errandIs(UNLINKED);
		when(client.readProposal(1)).thenReturn(json("{\"norms\":[{\"normId\":1,\"name\":\"Riksnorm 2026\"}]}"));

		final var types = service.types(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(types.getNorms()).extracting(NormberakningTypeOption::getCode, NormberakningTypeOption::getDisplayName).containsExactly(tuple("1", "Riksnorm 2026"));
		assertThat(types.getIncomeTypes()).isNotEmpty();
		assertThat(types.getCostTypes()).isNotEmpty().extracting(NormberakningTypeOption::getDisplayName).contains("Boendekostnad");
		assertThat(types.getLivingCostTypes()).isNotEmpty().doesNotContainAnyElementsOf(types.getCostTypes());
		verify(recorder).read(UNLINKED, "CALCULATION", "Läste normer i Lifecare");
	}

	@Test
	void offersNoNormsWhenLifecareCannotSayOrThereIsNoInsats() {
		errandIs(UNLINKED);
		when(client.readProposal(1)).thenThrow(Problem.valueOf(BAD_GATEWAY, "down"));

		assertThat(service.types(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID).getNorms()).isEmpty();

		errandIs(new LifecareErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, null, null, null, null, null, null));
		assertThat(service.types(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID).getNorms()).isEmpty();
		verifyNoInteractions(recorder);
	}

	@Test
	void offersLifecaresOwnTypesOnceTheBeräkningIsSavedThere() {
		errandIs(LINKED);
		final var types = NormberakningTypes.create();
		when(editService.readTypes(LINKED, 30)).thenReturn(types);

		assertThat(service.types(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isSameAs(types);
		verifyNoInteractions(client);
	}

	@Test
	void changesTheHeaderInCaremsDraftOrInLifecare() {
		final var input = NormHeaderInput.create().withNormId(1).withHasCustomHouseholdSize(true).withHouseholdSize(3);
		errandIs(UNLINKED);

		service.updateHeader(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, input);

		verify(calculationService).patchDraftHeader(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, input);
		verifyNoInteractions(editService);

		errandIs(LINKED);
		service.updateHeader(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, input);

		assertThat(appliedChange().path("householdSize").intValue()).isEqualTo(3);
	}

	@Test
	void addsARowInCaremsDraftBeforeTheBeräkningIsSavedInLifecare() {
		errandIs(UNLINKED);
		final var input = NormberakningRowInput.create().withTypeName("Lön").withApplicantCaseworkerAmount(BigDecimal.valueOf(100)).withCostType("RENT").withName("Barn");

		service.addRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "incomes", input);
		service.addRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "expenses", input);
		service.addRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "persons", input);

		final var income = ArgumentCaptor.forClass(NormIncomeInput.class);
		final var expense = ArgumentCaptor.forClass(NormExpenseInput.class);
		final var person = ArgumentCaptor.forClass(NormPersonInput.class);
		verify(draftRowService).addDraftIncome(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), income.capture());
		verify(draftRowService).addDraftExpense(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), expense.capture());
		verify(draftRowService).addDraftPerson(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), person.capture());
		assertThat(income.getValue().getTypeName()).isEqualTo("Lön");
		assertThat(expense.getValue().getCostType()).isEqualTo("RENT");
		assertThat(person.getValue().getName()).isEqualTo("Barn");
		verifyNoInteractions(editService);
	}

	@Test
	void addsAnIncomeInLifecareOnceSavedThere() {
		errandIs(LINKED);

		service.addRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "incomes", NormberakningRowInput.create().withTypeId(1).withApplicantCaseworkerAmount(BigDecimal.ONE));

		verifyNoInteractions(draftRowService);
		assertThatThrownBy(this::appliedChange).hasMessageContaining("Lön efter skatt finns redan");
	}

	@Test
	void addsAnUtgiftInLifecareOnceSavedThere() {
		errandIs(LINKED);

		service.addRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "expenses", NormberakningRowInput.create().withCostType("3").withCaseworkerAmount(BigDecimal.TEN));

		assertThat(objects(appliedChange(), "calculationExpenses")).hasSize(2);
	}

	@Test
	void leavesAddingAPersonToLifecare() {
		errandIs(LINKED);

		service.addRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "persons", NormberakningRowInput.create());

		refusedChange("Personer läggs till i hushållet i Lifecare");
	}

	@Test
	void changesARowInCaremsDraftBeforeTheBeräkningIsSavedInLifecare() {
		errandIs(UNLINKED);
		final var input = NormberakningRowInput.create().withNote("n");

		service.updateRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "incomes", ROW_ID, input);
		service.updateRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "expenses", ROW_ID, input);
		service.updateRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "persons", ROW_ID, input);

		verify(draftRowService).patchDraftIncome(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(ROW_ID), any());
		verify(draftRowService).patchDraftExpense(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(ROW_ID), any());
		verify(draftRowService).patchDraftPerson(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(ROW_ID), any());
	}

	@Test
	void changesAnIncomeInLifecareOnceSavedThere() {
		errandIs(LINKED);

		service.updateRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "incomes", "1", NormberakningRowInput.create().withApplicantCaseworkerAmount(BigDecimal.valueOf(6000)));

		assertThat(objects(appliedChange(), "calculationIncomes").getFirst().path("amountApplicant").intValue()).isEqualTo(6000);
	}

	@Test
	void changesAnUtgiftInLifecareOnceSavedThere() {
		errandIs(LINKED);

		service.updateRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "expenses", "E-3", NormberakningRowInput.create().withCaseworkerAmount(BigDecimal.valueOf(4500)));

		assertThat(objects(appliedChange(), "calculationExpenses").getFirst().path("approvedAmount").intValue()).isEqualTo(4500);
	}

	@Test
	void changesAMemberInLifecareOnceSavedThere() {
		errandIs(LINKED);

		service.updateRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "persons", "1", NormberakningRowInput.create().withCaseworkerDays(10));

		assertThat(objects(appliedChange(), "calculationPersons").getFirst().path("deviationDays").intValue()).isEqualTo(10);
	}

	@Test
	void softDeletesAndRestoresARowInCaremsDraft() {
		errandIs(UNLINKED);

		service.deleteRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "incomes", ROW_ID);
		service.deleteRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "expenses", ROW_ID);
		service.deleteRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "persons", ROW_ID);
		service.restoreRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "incomes", ROW_ID);
		service.restoreRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "expenses", ROW_ID);
		service.restoreRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "persons", ROW_ID);

		verify(draftRowService).setDraftIncomeDeleted(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ROW_ID, true);
		verify(draftRowService).setDraftExpenseDeleted(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ROW_ID, true);
		verify(draftRowService).setDraftPersonDeleted(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ROW_ID, true);
		verify(draftRowService).setDraftIncomeDeleted(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ROW_ID, false);
		verify(draftRowService).setDraftExpenseDeleted(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ROW_ID, false);
		verify(draftRowService).setDraftPersonDeleted(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ROW_ID, false);
	}

	@Test
	void removesAnIncomeInLifecareOnceSavedThere() {
		errandIs(LINKED);

		service.deleteRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "incomes", "1");

		assertThat(objects(appliedChange(), "calculationIncomes").getFirst().path("amountApplicant").intValue()).isZero();
	}

	@Test
	void removesAnUtgiftInLifecareOnceSavedThere() {
		errandIs(LINKED);

		service.deleteRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "expenses", "E-3");

		assertThat(objects(appliedChange(), "calculationExpenses").getFirst().path("approvedAmount").intValue()).isZero();
	}

	@Test
	void doesNotTakeAPersonOutOfABeräkningInLifecare() {
		errandIs(LINKED);

		service.deleteRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "persons", "1");

		refusedChange("Personer tas inte bort");
	}

	@Test
	void hasNoRestoreForABeräkningInLifecare() {
		errandIs(LINKED);

		assertThatThrownBy(() -> service.restoreRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "incomes", "1"))
			.hasFieldOrPropertyWithValue("status", UNPROCESSABLE_CONTENT);
		verifyNoInteractions(draftRowService, editService);
	}

	@Test
	void refusesAnUnknownSection() {
		errandIs(UNLINKED);
		final var input = NormberakningRowInput.create();

		assertThatThrownBy(() -> service.addRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "other", input)).hasFieldOrPropertyWithValue("status", BAD_REQUEST);
		assertThatThrownBy(() -> service.updateRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "other", ROW_ID, input)).hasFieldOrPropertyWithValue("status", BAD_REQUEST);
		assertThatThrownBy(() -> service.deleteRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "other", ROW_ID)).hasFieldOrPropertyWithValue("status", BAD_REQUEST);

		errandIs(LINKED);
		assertThatThrownBy(() -> service.addRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "other", input)).hasFieldOrPropertyWithValue("status", BAD_REQUEST);
		assertThatThrownBy(() -> service.updateRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "other", ROW_ID, input)).hasFieldOrPropertyWithValue("status", BAD_REQUEST);
		assertThatThrownBy(() -> service.deleteRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "other", ROW_ID)).hasFieldOrPropertyWithValue("status", BAD_REQUEST);
		verify(editService, never()).change(any(), anyInt(), any(), anyBoolean());
		verifyNoInteractions(draftRowService);
	}
}
