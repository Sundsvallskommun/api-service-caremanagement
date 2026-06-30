package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefc.PersonBasedCalculationCalculationIncomeTypeDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationExpenseTypeDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationNormDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationProposalDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationServiceDTO;
import generated.se.sundsvall.lifecarefc.PostCalculationBodyRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFcIntegration;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationHeader;
import se.sundsvall.caremanagement.lifecare.service.model.ClassifiedIncome;
import se.sundsvall.caremanagement.lifecare.service.model.DraftRow;
import se.sundsvall.caremanagement.lifecare.service.model.EffectiveExpense;
import se.sundsvall.caremanagement.lifecare.service.model.EffectiveIncome;
import se.sundsvall.caremanagement.lifecare.service.model.EffectivePerson;
import se.sundsvall.caremanagement.lifecare.service.model.PreviousHousehold;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekIncome;
import tools.jackson.databind.ObjectMapper;

import static java.time.Month.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculationServiceTest {

	private static final String APPLICANT = "199001011234";
	private static final YearMonth MONTH = YearMonth.of(2026, JUNE);

	@Mock
	private LifecareFcIntegration lifecareFcIntegrationMock;

	@Mock
	private LifecareEbCaseService lifecareEbCaseServiceMock;

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
	void buildAndPostFromClassifiedMapsCategoryToFcAndPosts() {
		when(objectMapperMock.readValue("[json]", ClassifiedIncome[].class)).thenReturn(new ClassifiedIncome[] {
			bostadsbidrag()
		});
		when(lifecareFcIntegrationMock.getCalculationProposal(APPLICANT)).thenReturn(proposal());
		when(lifecareFcIntegrationMock.createCalculation(any(PostCalculationBodyRequest.class))).thenReturn(4712);
		when(lifecareEbCaseServiceMock.previousCalculationIncomeTypes(APPLICANT, MONTH)).thenReturn(List.of("Bostadsbidrag"));

		final var result = service.buildAndPostFromClassified(APPLICANT, MONTH, "[json]");

		assertThat(result.calculationId()).isEqualTo(4712);
		assertThat(result.informationComplete()).isTrue();
		assertThat(result.missingIncomeTypes()).isEmpty();
		final ArgumentCaptor<PostCalculationBodyRequest> captor = ArgumentCaptor.forClass(PostCalculationBodyRequest.class);
		verify(lifecareFcIntegrationMock).createCalculation(captor.capture());
		assertThat(captor.getValue().getPersonId()).isEqualTo(APPLICANT);
		assertThat(captor.getValue().getServiceId()).isEqualTo(5);
		assertThat(captor.getValue().getCalculationIncomes()).hasSize(1);
		assertThat(captor.getValue().getCalculationIncomes().getFirst().getId()).isEqualTo(20);
	}

	@Test
	void reportsIncompleteWhenAPreviousIncomeTypeIsMissingThisMonth() {
		when(objectMapperMock.readValue("[json]", ClassifiedIncome[].class)).thenReturn(new ClassifiedIncome[] {
			bostadsbidrag()
		});
		when(lifecareFcIntegrationMock.getCalculationProposal(APPLICANT)).thenReturn(proposal());
		when(lifecareFcIntegrationMock.createCalculation(any(PostCalculationBodyRequest.class))).thenReturn(4712);
		when(lifecareEbCaseServiceMock.previousCalculationIncomeTypes(APPLICANT, MONTH)).thenReturn(List.of("Bostadsbidrag", "Dagersättning"));

		final var result = service.buildAndPostFromClassified(APPLICANT, MONTH, "[json]");

		assertThat(result.informationComplete()).isFalse();
		assertThat(result.missingIncomeTypes()).containsExactly("Dagersättning");
	}

	@Test
	void treatsCompletenessAsCompleteWhenPreviousLookupFails() {
		when(objectMapperMock.readValue("[json]", ClassifiedIncome[].class)).thenReturn(new ClassifiedIncome[] {
			bostadsbidrag()
		});
		when(lifecareFcIntegrationMock.getCalculationProposal(APPLICANT)).thenReturn(proposal());
		when(lifecareFcIntegrationMock.createCalculation(any(PostCalculationBodyRequest.class))).thenReturn(4712);
		when(lifecareEbCaseServiceMock.previousCalculationIncomeTypes(APPLICANT, MONTH)).thenThrow(new RuntimeException("FC down"));

		final var result = service.buildAndPostFromClassified(APPLICANT, MONTH, "[json]");

		assertThat(result.calculationId()).isEqualTo(4712);
		assertThat(result.informationComplete()).isTrue();
		assertThat(result.missingIncomeTypes()).isEmpty();
	}

	@Test
	void buildDraftReturnsRowsAndCompletenessWithoutPosting() {
		when(objectMapperMock.readValue("[json]", ClassifiedIncome[].class)).thenReturn(new ClassifiedIncome[] {
			bostadsbidrag()
		});
		when(lifecareFcIntegrationMock.getCalculationProposal(APPLICANT)).thenReturn(proposal());
		when(lifecareEbCaseServiceMock.previousCalculationIncomeTypes(APPLICANT, MONTH)).thenReturn(List.of("Bostadsbidrag"));

		final var result = service.buildDraft(APPLICANT, MONTH, "[json]");

		assertThat(result.informationComplete()).isTrue();
		assertThat(result.missingIncomeTypes()).isEmpty();
		assertThat(result.rows()).hasSize(1);
		assertThat(result.rows().getFirst().typeId()).isEqualTo(20);
		assertThat(result.rows().getFirst().typeName()).isEqualTo("Bostadsbidrag");
		assertThat(result.rows().getFirst().applicantAmount()).isEqualTo(1850.0);
		verify(lifecareFcIntegrationMock, org.mockito.Mockito.never()).createCalculation(any());
	}

	@Test
	void postDraftRowsAssemblesFromRowsAndPosts() {
		when(lifecareFcIntegrationMock.getCalculationProposal(APPLICANT)).thenReturn(proposal());
		when(lifecareFcIntegrationMock.createCalculation(any(PostCalculationBodyRequest.class))).thenReturn(4713);

		final var rows = List.of(new DraftRow(20, "Bostadsbidrag", 1850.0, "2026-05-15T00:00:00Z", null, null, "SSBTEK: Bostadsbidrag"));
		final var calculationId = service.postDraftRows(APPLICANT, MONTH, rows);

		assertThat(calculationId).isEqualTo(4713);
		final ArgumentCaptor<PostCalculationBodyRequest> captor = ArgumentCaptor.forClass(PostCalculationBodyRequest.class);
		verify(lifecareFcIntegrationMock).createCalculation(captor.capture());
		assertThat(captor.getValue().getCalculationIncomes()).hasSize(1);
		assertThat(captor.getValue().getCalculationIncomes().getFirst().getId()).isEqualTo(20);
		assertThat(captor.getValue().getCalculationIncomes().getFirst().getApplicantAmount()).isEqualTo(1850.0);
	}

	@Test
	void incomeLinesResolvesPerRecipientRows() {
		when(objectMapperMock.readValue("[json]", ClassifiedIncome[].class)).thenReturn(new ClassifiedIncome[] {
			bostadsbidrag()
		});
		when(lifecareFcIntegrationMock.getCalculationProposal(APPLICANT)).thenReturn(proposal());

		final var lines = service.incomeLines(APPLICANT, "[json]");

		assertThat(lines).singleElement().satisfies(line -> {
			assertThat(line.typeId()).isEqualTo(20);
			assertThat(line.typeName()).isEqualTo("Bostadsbidrag");
			assertThat(line.recipient()).isEqualTo("APPLICANT");
			assertThat(line.amount()).isEqualByComparingTo("1850");
		});
	}

	@Test
	void applicationIncomeLinesResolveAgainstProposal() {
		when(lifecareFcIntegrationMock.getCalculationProposal(APPLICANT)).thenReturn(proposal()
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(11).name("Lön efter skatt")));

		final var lines = service.applicationIncomeLines(APPLICANT, List.of(
			new se.sundsvall.caremanagement.lifecare.service.model.ApplicationIncome("SALARY", new BigDecimal("18500"), LocalDate.of(2026, MAY, 25), ApplicantRole.APPLICANT)));

		assertThat(lines).singleElement().satisfies(line -> {
			assertThat(line.typeId()).isEqualTo(11);
			assertThat(line.typeName()).isEqualTo("Lön efter skatt");
			assertThat(line.recipient()).isEqualTo("APPLICANT");
			assertThat(line.amount()).isEqualByComparingTo("18500");
			assertThat(line.note()).isEqualTo("Ansökan");
		});
	}

	@Test
	void completenessReportsMissingPreviousTypes() {
		when(objectMapperMock.readValue("[json]", ClassifiedIncome[].class)).thenReturn(new ClassifiedIncome[] {
			bostadsbidrag()
		});
		when(lifecareFcIntegrationMock.getCalculationProposal(APPLICANT)).thenReturn(proposal());
		when(lifecareEbCaseServiceMock.previousCalculationIncomeTypes(APPLICANT, MONTH)).thenReturn(List.of("Bostadsbidrag", "Dagersättning"));

		final var completeness = service.completeness(APPLICANT, MONTH, "[json]");

		assertThat(completeness.informationComplete()).isFalse();
		assertThat(completeness.missingIncomeTypes()).containsExactly("Dagersättning");
	}

	@Test
	void selectNormIdPicksTheNormCoveringTheMonth() {
		when(lifecareFcIntegrationMock.getCalculationProposal(APPLICANT)).thenReturn(proposal()
			.addNormsItem(new PersonBasedCalculationNormDTO().id(99).fromDate("2026-01-01").toDate("2026-12-31")));

		assertThat(service.selectNormId(APPLICANT, MONTH)).isEqualTo(99);
	}

	@Test
	void previousHouseholdDelegatesToLifecare() {
		final var household = new PreviousHousehold(Set.of("p1", "p2"), 2, 9000.0, 4500.0);
		when(lifecareEbCaseServiceMock.previousHousehold(APPLICANT, MONTH)).thenReturn(household);

		assertThat(service.previousHousehold(APPLICANT, MONTH)).isSameAs(household);
	}

	@Test
	void commitEffectiveAssemblesIncomesExpensesAndPersonsAndPosts() {
		when(lifecareFcIntegrationMock.getCalculationProposal(APPLICANT)).thenReturn(proposal()
			.addCalculationExpenseTypesItem(new PersonBasedCalculationExpenseTypeDTO().id(42).name("Rent")));
		when(lifecareFcIntegrationMock.createCalculation(any(PostCalculationBodyRequest.class))).thenReturn(5000);

		final var incomes = List.of(new EffectiveIncome(20, 1850.0, null, null, null, "SSBTEK"));
		final var expenses = List.of(
			new EffectiveExpense("RENT", "EXPENSE", 9000.0, 8000.0, null), // resolves to FC id 42
			new EffectiveExpense("UNMAPPED_NONSENSE", "EXPENSE", 100.0, 100.0, null)); // skipped (no FC id)
		final var persons = List.of(new EffectivePerson("p1", 30, null, null));

		final var calculationId = service.commitEffective(APPLICANT, MONTH, new CalculationHeader(7, null, null, null, null, null), incomes, expenses, persons);

		assertThat(calculationId).isEqualTo(5000);
		final ArgumentCaptor<PostCalculationBodyRequest> captor = ArgumentCaptor.forClass(PostCalculationBodyRequest.class);
		verify(lifecareFcIntegrationMock).createCalculation(captor.capture());
		final var body = captor.getValue();
		assertThat(body.getNormId()).isEqualTo(7); // override applied
		assertThat(body.getCalculationIncomes()).singleElement().satisfies(income -> assertThat(income.getId()).isEqualTo(20));
		assertThat(body.getCalculationExpenses()).singleElement().satisfies(expense -> {
			assertThat(expense.getId()).isEqualTo(42);
			assertThat(expense.getAmount()).isEqualTo(9000.0);
			assertThat(expense.getApprovedAmount()).isEqualTo(8000.0);
		});
		assertThat(body.getCalculationPersons()).singleElement().satisfies(person -> {
			assertThat(person.getPersonId()).isEqualTo("p1");
			assertThat(person.getNumberOfDays()).isEqualTo(30);
		});
	}
}
