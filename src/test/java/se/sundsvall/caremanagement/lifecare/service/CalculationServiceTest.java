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
import tools.jackson.databind.ObjectMapper;

import static java.time.Month.JUNE;
import static java.time.Month.MAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculationServiceTest {

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
	void incomeLinesResolvesPerRecipientRows() {
		when(objectMapperMock.readValue("[json]", ClassifiedIncome[].class)).thenReturn(new ClassifiedIncome[] {
			bostadsbidrag()
		});
		when(lifecareFamilyCareIntegrationMock.getCalculationProposal(APPLICANT)).thenReturn(proposal());

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
		when(lifecareFamilyCareIntegrationMock.getCalculationProposal(APPLICANT)).thenReturn(proposal()
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(11).name("Lön efter skatt")));

		final var lines = service.applicationIncomeLines(APPLICANT, List.of(
			new ApplicationIncome("SALARY", new BigDecimal("18500"), LocalDate.of(2026, MAY, 25), ApplicantRole.APPLICANT)));

		assertThat(lines).singleElement().satisfies(line -> {
			assertThat(line.typeId()).isEqualTo(11);
			assertThat(line.typeName()).isEqualTo("Lön efter skatt");
			assertThat(line.recipient()).isEqualTo("APPLICANT");
			assertThat(line.amount()).isEqualByComparingTo("18500");
			assertThat(line.note()).isEqualTo("Ansökan");
		});
	}

	@Test
	void completenessTreatedAsCompleteWhenThePreviousLookupFails() {
		// Best-effort: a Lifecare outage must not wedge the financial assistance process on an incomplete verdict.
		when(objectMapperMock.readValue("[json]", ClassifiedIncome[].class)).thenReturn(new ClassifiedIncome[] {
			bostadsbidrag()
		});
		when(lifecareFamilyCareIntegrationMock.getCalculationProposal(APPLICANT)).thenReturn(proposal());
		when(lifecareCaseServiceMock.previousCalculationIncomeTypes(APPLICANT, MONTH)).thenThrow(new RuntimeException("FamilyCare down"));

		final var completeness = service.completeness(APPLICANT, MONTH, "[json]");

		assertThat(completeness.informationComplete()).isTrue();
		assertThat(completeness.missingIncomeTypes()).isEmpty();
	}

	@Test
	void completenessReportsMissingPreviousTypes() {
		when(objectMapperMock.readValue("[json]", ClassifiedIncome[].class)).thenReturn(new ClassifiedIncome[] {
			bostadsbidrag()
		});
		when(lifecareFamilyCareIntegrationMock.getCalculationProposal(APPLICANT)).thenReturn(proposal());
		when(lifecareCaseServiceMock.previousCalculationIncomeTypes(APPLICANT, MONTH)).thenReturn(List.of("Bostadsbidrag", "Dagersättning"));

		final var completeness = service.completeness(APPLICANT, MONTH, "[json]");

		assertThat(completeness.informationComplete()).isFalse();
		assertThat(completeness.missingIncomeTypes()).containsExactly("Dagersättning");
	}

	@Test
	void selectNormIdPicksTheNormCoveringTheMonth() {
		when(lifecareFamilyCareIntegrationMock.getCalculationProposal(APPLICANT)).thenReturn(proposal()
			.addNormsItem(new PersonBasedCalculationNormDTO().id(99).fromDate("2026-01-01").toDate("2026-12-31")));

		assertThat(service.selectNormId(APPLICANT, MONTH)).isEqualTo(99);
	}

	@Test
	void commitEffectiveAssemblesIncomesExpensesAndPersonsAndPosts() {
		when(lifecareFamilyCareIntegrationMock.getCalculationProposal(APPLICANT)).thenReturn(proposal()
			.addCalculationExpenseTypesItem(new PersonBasedCalculationExpenseTypeDTO().id(42).name("Rent")));
		when(lifecareFamilyCareIntegrationMock.createCalculation(any(PostCalculationBodyRequest.class))).thenReturn(5000);

		final var incomes = List.of(new EffectiveIncome(20, BigDecimal.valueOf(1850.0), null, null, null, "SSBTEK"));
		final var expenses = List.of(
			new EffectiveExpense("RENT", "EXPENSE", BigDecimal.valueOf(9000.0), BigDecimal.valueOf(8000.0), null), // resolves to FamilyCare id 42
			new EffectiveExpense("UNMAPPED_NONSENSE", "EXPENSE", BigDecimal.valueOf(100.0), BigDecimal.valueOf(100.0), null)); // skipped (no FamilyCare id)
		final var persons = List.of(new EffectivePerson("p1", 30, null, null));

		final var calculationId = service.commitEffective(APPLICANT, MONTH, new CalculationHeader(7, null, null, null, null, null), incomes, expenses, persons);

		assertThat(calculationId).isEqualTo(5000);
		final ArgumentCaptor<PostCalculationBodyRequest> captor = ArgumentCaptor.forClass(PostCalculationBodyRequest.class);
		verify(lifecareFamilyCareIntegrationMock).createCalculation(captor.capture());
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
