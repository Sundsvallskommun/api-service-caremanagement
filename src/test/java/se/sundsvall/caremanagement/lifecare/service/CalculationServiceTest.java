package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationCalculationIncomeTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationExpenseTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationNormDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationProposalDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationServiceDTO;
import generated.se.sundsvall.lifecarefamilycare.PostCalculationBodyRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFamilyCareIntegration;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicationIncome;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationHeader;
import se.sundsvall.caremanagement.lifecare.service.model.ClassifiedIncome;
import se.sundsvall.caremanagement.lifecare.service.model.EffectiveExpense;
import se.sundsvall.caremanagement.lifecare.service.model.EffectiveIncome;
import se.sundsvall.caremanagement.lifecare.service.model.EffectivePerson;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekIncome;
import se.sundsvall.dept44.problem.ThrowableProblem;
import tools.jackson.databind.ObjectMapper;

import static java.time.Month.JUNE;
import static java.time.Month.MAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculationServiceTest {

	private static final String MUNICIPALITY_ID = "2281";

	private static final String APPLICANT = "199001011234";
	private static final YearMonth MONTH = YearMonth.of(2026, JUNE);

	@Mock
	private LifecareFamilyCareIntegration lifecareFamilyCareIntegrationMock;

	@Mock
	private LifecareCaseService lifecareCaseServiceMock;

	@Mock
	private ObjectMapper objectMapperMock;

	@InjectMocks
	private CalculationService service;

	private static ClassifiedIncome bostadsbidrag() {
		return new ClassifiedIncome(
			new SsbtekIncome("Bostadsbidrag", null, "Månad", new BigDecimal("1850"), LocalDate.of(2026, MAY, 15), ApplicantRole.APPLICANT),
			"TA_MED_KVITTNING", "Bostadsbidrag", false, "Ta med kvittning");
	}

	private static PersonBasedCalculationProposalDTO proposal() {
		return new PersonBasedCalculationProposalDTO()
			.addServicesItem(new PersonBasedCalculationServiceDTO().id(5))
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(20).name("Bostadsbidrag"));
	}

	@Test
	void applicationIncomeLinesResolvesTheDeclaredIncomesAgainstTheProposal() {
		final var proposal = proposal().addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(30).name("Swish/Insättningar/Överföringar"));
		final var swish = new ApplicationIncome("SWISH_DEPOSITS", null, new BigDecimal("599"), null, "Swish/kontoinsättningar");
		final var salary = new ApplicationIncome("SALARY", null, new BigDecimal("6788"), null, "Lön");
		when(lifecareFamilyCareIntegrationMock.getCalculationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal);

		final var result = service.applicationIncomeLines(MUNICIPALITY_ID, APPLICANT, List.of(swish, salary));

		assertThat(result.lines()).singleElement().satisfies(line -> {
			assertThat(line.typeId()).isEqualTo(30);
			assertThat(line.amount()).isEqualByComparingTo("599");
			assertThat(line.note()).isEqualTo("Ansökan: Swish/kontoinsättningar");
		});
		assertThat(result.untransferable()).containsExactly(salary);
	}

	@Test
	void incomeLinesResolvesPerRecipientRows() {
		when(objectMapperMock.readValue("[json]", ClassifiedIncome[].class)).thenReturn(new ClassifiedIncome[] {
			bostadsbidrag()
		});
		when(lifecareFamilyCareIntegrationMock.getCalculationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal());
		when(lifecareCaseServiceMock.previousCalculationIncomeTypes(MUNICIPALITY_ID, APPLICANT, MONTH)).thenReturn(List.of());

		final var lines = service.incomeLines(MUNICIPALITY_ID, APPLICANT, MONTH, "[json]", Map.of());

		assertThat(lines).singleElement().satisfies(line -> {
			assertThat(line.typeId()).isEqualTo(20);
			assertThat(line.typeName()).isEqualTo("Bostadsbidrag");
			assertThat(line.recipient()).isEqualTo("APPLICANT");
			assertThat(line.amount()).isEqualByComparingTo("1850");
		});
	}

	@Test
	void lateTransferredComparisonIncomesReportsWhatTheTransferPickedUp() {
		final var comparisonPeriod = new ClassifiedIncome(
			new SsbtekIncome("Underhållsstöd", null, "Månad", new BigDecimal("1673"), LocalDate.of(2026, MAY, 15), ApplicantRole.APPLICANT),
			"TA_MED", "Underhållsstöd", false, "Ta med", true);
		when(objectMapperMock.readValue("[json]", ClassifiedIncome[].class)).thenReturn(new ClassifiedIncome[] {
			comparisonPeriod, bostadsbidrag()
		});
		when(lifecareCaseServiceMock.previousCalculationIncomeTypes(MUNICIPALITY_ID, APPLICANT, MONTH)).thenReturn(List.of());

		final var late = service.lateTransferredComparisonIncomes(MUNICIPALITY_ID, APPLICANT, MONTH, "[json]");

		// Only the comparison-period income the previous calculation lacked — the control-period one is not a late arrival.
		assertThat(late).singleElement().satisfies(income -> assertThat(income.income().benefit()).isEqualTo("Underhållsstöd"));
	}

	@Test
	void untransferableIncomesNamesWhatTheProposalHasNoTypeFor() {
		final var barnbidrag = new ClassifiedIncome(
			new SsbtekIncome("Allmänt barnbidrag", null, "Månad", new BigDecimal("1250"), LocalDate.of(2026, MAY, 20), ApplicantRole.APPLICANT),
			"TA_MED", "Barnbidrag", false, "Ta med");
		when(objectMapperMock.readValue("[json]", ClassifiedIncome[].class)).thenReturn(new ClassifiedIncome[] {
			bostadsbidrag(), barnbidrag
		});
		when(lifecareFamilyCareIntegrationMock.getCalculationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal());
		when(lifecareCaseServiceMock.previousCalculationIncomeTypes(MUNICIPALITY_ID, APPLICANT, MONTH)).thenReturn(List.of());

		// proposal() offers only Bostadsbidrag, so the barnbidrag is exactly what incomeLines leaves out.
		assertThat(service.untransferableIncomes(MUNICIPALITY_ID, APPLICANT, MONTH, "[json]"))
			.singleElement().satisfies(income -> assertThat(income.income().benefit()).isEqualTo("Allmänt barnbidrag"));
	}

	@Test
	void untransferableIncomesSkipsWhatThePreviousMonthAlreadyTook() {
		final var comparisonPeriod = new ClassifiedIncome(
			new SsbtekIncome("Studiemedel", null, "Månad", new BigDecimal("3000"), LocalDate.of(2026, MAY, 15), ApplicantRole.APPLICANT),
			"TA_MED", "Studiemedel", false, "Ta med", true);
		when(objectMapperMock.readValue("[json]", ClassifiedIncome[].class)).thenReturn(new ClassifiedIncome[] {
			comparisonPeriod
		});
		when(lifecareFamilyCareIntegrationMock.getCalculationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal());
		when(lifecareCaseServiceMock.previousCalculationIncomeTypes(MUNICIPALITY_ID, APPLICANT, MONTH)).thenReturn(List.of("Studiemedel"));

		// Not transferred this month at all, so it is not missing from the draft either.
		assertThat(service.untransferableIncomes(MUNICIPALITY_ID, APPLICANT, MONTH, "[json]")).isEmpty();
	}

	@Test
	void lateTransferredComparisonIncomesIsEmptyWhenThePreviousMonthAlreadyTookThem() {
		final var comparisonPeriod = new ClassifiedIncome(
			new SsbtekIncome("Underhållsstöd", null, "Månad", new BigDecimal("1673"), LocalDate.of(2026, MAY, 15), ApplicantRole.APPLICANT),
			"TA_MED", "Underhållsstöd", false, "Ta med", true);
		when(objectMapperMock.readValue("[json]", ClassifiedIncome[].class)).thenReturn(new ClassifiedIncome[] {
			comparisonPeriod
		});
		when(lifecareCaseServiceMock.previousCalculationIncomeTypes(MUNICIPALITY_ID, APPLICANT, MONTH)).thenReturn(List.of("Underhållsstöd"));

		// Nothing moved, so there is nothing to warn about — the money was already counted last month.
		assertThat(service.lateTransferredComparisonIncomes(MUNICIPALITY_ID, APPLICANT, MONTH, "[json]")).isEmpty();
	}

	@Test
	void incomeLinesTransfersTheComparisonPeriodUnfilteredWhenThePreviousMonthCannotBeRead() {
		when(objectMapperMock.readValue("[json]", ClassifiedIncome[].class)).thenReturn(new ClassifiedIncome[] {
			bostadsbidrag()
		});
		when(lifecareFamilyCareIntegrationMock.getCalculationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal());
		when(lifecareCaseServiceMock.previousCalculationIncomeTypes(MUNICIPALITY_ID, APPLICANT, MONTH))
			.thenThrow(new IllegalStateException("Lifecare unavailable"));

		// an income counted twice surfaces as a duplicate warning; one silently withheld surfaces as nothing
		assertThat(service.incomeLines(MUNICIPALITY_ID, APPLICANT, MONTH, "[json]", Map.of())).hasSize(1);
	}

	@Test
	void completenessTreatedAsCompleteWhenThePreviousLookupFails() {
		// Best-effort: a Lifecare outage must not wedge the financial assistance process on an incomplete verdict.
		when(objectMapperMock.readValue("[json]", ClassifiedIncome[].class)).thenReturn(new ClassifiedIncome[] {
			bostadsbidrag()
		});
		when(lifecareFamilyCareIntegrationMock.getCalculationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal());
		when(lifecareCaseServiceMock.previousCalculationIncomeTypes(MUNICIPALITY_ID, APPLICANT, MONTH)).thenThrow(new RuntimeException("FamilyCare down"));

		final var completeness = service.completeness(MUNICIPALITY_ID, APPLICANT, MONTH, "[json]");

		assertThat(completeness.informationComplete()).isTrue();
		assertThat(completeness.missingIncomeTypes()).isEmpty();
	}

	@Test
	void completenessReportsMissingPreviousTypes() {
		when(objectMapperMock.readValue("[json]", ClassifiedIncome[].class)).thenReturn(new ClassifiedIncome[] {
			bostadsbidrag()
		});
		when(lifecareFamilyCareIntegrationMock.getCalculationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal());
		when(lifecareCaseServiceMock.previousCalculationIncomeTypes(MUNICIPALITY_ID, APPLICANT, MONTH)).thenReturn(List.of("Bostadsbidrag", "Dagersättning"));

		final var completeness = service.completeness(MUNICIPALITY_ID, APPLICANT, MONTH, "[json]");

		assertThat(completeness.informationComplete()).isFalse();
		assertThat(completeness.missingIncomeTypes()).containsExactly("Dagersättning");
	}

	@Test
	void selectNormIdPicksTheNormCoveringTheMonth() {
		when(lifecareFamilyCareIntegrationMock.getCalculationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal()
			.addNormsItem(new PersonBasedCalculationNormDTO().id(99).fromDate("2026-01-01").toDate("2026-12-31")));

		assertThat(service.selectNormId(MUNICIPALITY_ID, APPLICANT, MONTH, List.of(), List.of()))
			.isEqualTo(new CalculationService.NormChoice(99, false));
	}

	@Test
	void selectNormIdPrefersThePreviousNormAndSaysSo() {
		when(lifecareFamilyCareIntegrationMock.getCalculationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal()
			.addNormsItem(new PersonBasedCalculationNormDTO().id(4).name("Matnorm 2026").fromDate("2026-01-01").toDate("2026-12-31"))
			.addNormsItem(new PersonBasedCalculationNormDTO().id(1).name("Riksnorm 2026").fromDate("2026-01-01").toDate("2026-12-31")));

		assertThat(service.selectNormId(MUNICIPALITY_ID, APPLICANT, MONTH, List.of("Matnorm"), List.of("Riksnorm")))
			.isEqualTo(new CalculationService.NormChoice(4, true));
		// a previous norm the month does not offer: the application's is chosen, and the choice says it was not preferred
		assertThat(service.selectNormId(MUNICIPALITY_ID, APPLICANT, MONTH, List.of("Specnorm"), List.of("Riksnorm")))
			.isEqualTo(new CalculationService.NormChoice(1, false));
		verify(lifecareFamilyCareIntegrationMock, times(2)).getCalculationProposal(MUNICIPALITY_ID, APPLICANT);
	}

	@Test
	void commitEffectiveCutsANoteToWhatLifecareAccepts() {
		// FamilyCare refuses the whole calculation (400) when a row's note is longer than 80 characters.
		when(lifecareFamilyCareIntegrationMock.getCalculationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal());
		when(lifecareFamilyCareIntegrationMock.createCalculation(eq(MUNICIPALITY_ID), any(PostCalculationBodyRequest.class))).thenReturn(5000);
		final var longNote = "Ansökan: Annan inkomst (lån, spelvinst, försörjning av tillgång, gåva, kontanter)";
		final var incomes = List.of(new EffectiveIncome(20, "Bostadsbidrag", BigDecimal.valueOf(250.0), null, null, null, longNote));

		service.commitEffective(MUNICIPALITY_ID, APPLICANT, MONTH, new CalculationHeader(7, null, null, null, null, null, null), incomes, List.of(), List.of());

		final ArgumentCaptor<PostCalculationBodyRequest> captor = ArgumentCaptor.forClass(PostCalculationBodyRequest.class);
		verify(lifecareFamilyCareIntegrationMock).createCalculation(eq(MUNICIPALITY_ID), captor.capture());
		assertThat(captor.getValue().getCalculationIncomes()).singleElement()
			.satisfies(income -> assertThat(income.getNote()).hasSize(80).isEqualTo(longNote.substring(0, 80)));
	}

	@Test
	void commitEffectiveResolvesACaseworkerIncomeWithoutTypeIdByItsName() {
		when(lifecareFamilyCareIntegrationMock.getCalculationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal()
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(11).name("Lön efter skatt")));
		when(lifecareFamilyCareIntegrationMock.createCalculation(eq(MUNICIPALITY_ID), any(PostCalculationBodyRequest.class))).thenReturn(5000);
		final var incomes = List.of(new EffectiveIncome(null, " lön EFTER skatt ", BigDecimal.valueOf(5000.0), null, null, null, null));

		service.commitEffective(MUNICIPALITY_ID, APPLICANT, MONTH, new CalculationHeader(7, null, null, null, null, null, null), incomes, List.of(), List.of());

		final ArgumentCaptor<PostCalculationBodyRequest> captor = ArgumentCaptor.forClass(PostCalculationBodyRequest.class);
		verify(lifecareFamilyCareIntegrationMock).createCalculation(eq(MUNICIPALITY_ID), captor.capture());
		assertThat(captor.getValue().getCalculationIncomes()).singleElement().satisfies(income -> assertThat(income.getId()).isEqualTo(11));
	}

	@Test
	void commitEffectiveRefusesAnIncomeWhoseTypeCannotBeResolved() {
		when(lifecareFamilyCareIntegrationMock.getCalculationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal());
		final var incomes = List.of(new EffectiveIncome(null, "Okänd inkomst", BigDecimal.valueOf(5000.0), null, null, null, null));
		final var header = new CalculationHeader(7, null, null, null, null, null, null);

		assertThatThrownBy(() -> service.commitEffective(MUNICIPALITY_ID, APPLICANT, MONTH, header, incomes, List.of(), List.of()))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessage("Bad Request: Income row 'Okänd inkomst' has no Lifecare income type id and its name matches none of Lifecare's income types");
		verify(lifecareFamilyCareIntegrationMock, never()).createCalculation(any(), any());
	}

	@Test
	void commitEffectiveAssemblesIncomesExpensesAndPersonsAndPosts() {
		when(lifecareFamilyCareIntegrationMock.getCalculationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal()
			.addCalculationExpenseTypesItem(new PersonBasedCalculationExpenseTypeDTO().id(42).name("Boendekostnad")));
		when(lifecareFamilyCareIntegrationMock.createCalculation(eq(MUNICIPALITY_ID), any(PostCalculationBodyRequest.class))).thenReturn(5000);

		final var incomes = List.of(new EffectiveIncome(20, "Bostadsbidrag", BigDecimal.valueOf(1850.0), null, null, null, "SSBTEK"));
		final var expenses = List.of(
			new EffectiveExpense("RENT", "EXPENSE", BigDecimal.valueOf(9000.0), BigDecimal.valueOf(8000.0), null), // resolves to FamilyCare id 42
			new EffectiveExpense("UNMAPPED_NONSENSE", "EXPENSE", BigDecimal.valueOf(100.0), BigDecimal.valueOf(100.0), null)); // skipped (no FamilyCare id)
		final var persons = List.of(new EffectivePerson("p1", 30, null, null), new EffectivePerson("p2", 12, null, null));

		final var calculationId = service.commitEffective(MUNICIPALITY_ID, APPLICANT, MONTH, new CalculationHeader(7, null, null, null, null, null, null), incomes, expenses, persons);

		assertThat(calculationId).isEqualTo(5000);
		final ArgumentCaptor<PostCalculationBodyRequest> captor = ArgumentCaptor.forClass(PostCalculationBodyRequest.class);
		verify(lifecareFamilyCareIntegrationMock).createCalculation(eq(MUNICIPALITY_ID), captor.capture());
		final var body = captor.getValue();
		assertThat(body.getNormId()).isEqualTo(7); // override applied
		assertThat(body.getCalculationIncomes()).singleElement().satisfies(income -> assertThat(income.getId()).isEqualTo(20));
		assertThat(body.getCalculationExpenses()).singleElement().satisfies(expense -> {
			assertThat(expense.getId()).isEqualTo(42);
			assertThat(expense.getAmount()).isEqualTo(9000.0);
			assertThat(expense.getApprovedAmount()).isEqualTo(8000.0);
		});
		// The household row carries careM's partyId by design — the direct route resolves it to a personal identity
		// number in LifecareFamilyCareIntegration.createCalculation, the integrator route wants it unchanged.
		assertThat(body.getCalculationPersons())
			.extracting(person -> person.getPersonId(), person -> person.getNumberOfDays())
			.containsExactly(tuple("p1", null), tuple("p2", 12));
		// No household size on the draft: not custom, as many as the persons posted — left unset, Lifecare stores 0 and
		// computes no gemensamma kostnader.
		assertThat(body.getHasCustomHouseholdSize()).isFalse();
		assertThat(body.getHouseholdSize()).isEqualTo(2);
	}

	@Test
	void commitEffectiveKeepsTheDraftsCustomHouseholdSize() {
		when(lifecareFamilyCareIntegrationMock.getCalculationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal());
		when(lifecareFamilyCareIntegrationMock.createCalculation(eq(MUNICIPALITY_ID), any(PostCalculationBodyRequest.class))).thenReturn(5001);
		final var persons = List.of(new EffectivePerson("p1", 30, null, null));

		service.commitEffective(MUNICIPALITY_ID, APPLICANT, MONTH, new CalculationHeader(7, null, null, null, true, 3, null), List.of(), List.of(), persons);

		final ArgumentCaptor<PostCalculationBodyRequest> captor = ArgumentCaptor.forClass(PostCalculationBodyRequest.class);
		verify(lifecareFamilyCareIntegrationMock).createCalculation(eq(MUNICIPALITY_ID), captor.capture());
		assertThat(captor.getValue().getHasCustomHouseholdSize()).isTrue();
		assertThat(captor.getValue().getHouseholdSize()).isEqualTo(3);
	}

	/**
	 * FamilyCare prorates the norm by any NumberOfDays it gets, and refuses one above the difference between the
	 * period's first and last day. careM counts a full month as 30 whatever its length, so a full month goes without
	 * days — as Lifecare's own web app sends it — and a part month is capped at the FamilyCare edge.
	 */
	@Test
	void sendsNoDaysForAFullMonthAndCapsAPartMonth() {
		when(lifecareFamilyCareIntegrationMock.getCalculationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal());
		when(lifecareFamilyCareIntegrationMock.createCalculation(eq(MUNICIPALITY_ID), any(PostCalculationBodyRequest.class))).thenReturn(6000);

		final var february = YearMonth.of(2026, 2); // 2026-02-01–2026-02-28 is 27
		final var persons = List.of(
			new EffectivePerson("full", 30, null, null),
			new EffectivePerson("partial", 12, null, null),
			new EffectivePerson("capped", 29, null, null),
			new EffectivePerson("unknown", null, null, null));

		service.commitEffective(MUNICIPALITY_ID, APPLICANT, february, new CalculationHeader(7, null, null, null, null, null, null), List.of(), List.of(), persons);

		final ArgumentCaptor<PostCalculationBodyRequest> captor = ArgumentCaptor.forClass(PostCalculationBodyRequest.class);
		verify(lifecareFamilyCareIntegrationMock).createCalculation(eq(MUNICIPALITY_ID), captor.capture());
		assertThat(captor.getValue().getCalculationPersons())
			.extracting(person -> person.getPersonId(), person -> person.getNumberOfDays())
			.containsExactly(tuple("full", null), tuple("partial", 12), tuple("capped", 27), tuple("unknown", null));
	}
}
