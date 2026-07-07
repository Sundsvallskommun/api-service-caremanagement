package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedCalculationDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedDecisionDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedDocumentDTO;
import generated.se.sundsvall.lifecarefc.CommonCalculationExpenseDTO;
import generated.se.sundsvall.lifecarefc.CommonCalculationIncomeDTO;
import generated.se.sundsvall.lifecarefc.CommonCalculationSpecialExpenseDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationPersonDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedDecisionDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedDecisionPersonDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedDocumentDTO;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFcIntegration;

import static java.time.Month.JANUARY;
import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LifecareCaseHistoryServiceTest {

	private static final String PERSON_ID = "199001011234";
	private static final LocalDate FROM = LocalDate.of(2026, JANUARY, 1);
	private static final LocalDate TO = LocalDate.of(2026, JUNE, 30);

	@Mock
	private LifecareFcIntegration lifecareFcIntegrationMock;

	@InjectMocks
	private LifecareCaseHistoryService service;

	@Test
	void listCalculationsMapsHeaderAndRows() {
		final var dto = new PersonBasedCalculationDTO()
			.id(7001).norm("Riksnorm 2026").fromDate("2026-06-01").toDate("2026-06-30")
			.incomeSum(12000.0).expenseSum(9500.0).specialExpenseSum(500.0).normSum(10500.0)
			.commonHouseholdCost(1200.0).familyCost(800.0).balance(-2000.0).totalSum(8500.0)._final(true)
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId("200001011234").name("Barn").amount(4500.0).deviationFromDate("2026-06-01").deviationToDate("2026-06-30"))
			.addCalculationIncomesDTOsItem(new CommonCalculationIncomeDTO().type("Lön").amountApplicant(12000.0).applicantSearchDate("2026-05-15").amountCoApplicant(0.0).coApplicantSearchDate(null))
			.addCalculationExpensesDTOsItem(new CommonCalculationExpenseDTO().type("Hyra").appliedAmount(7500.0).approvedAmount(7000.0))
			.addCalculationSpecialExpensesDTOsItem(new CommonCalculationSpecialExpenseDTO().type("Tandvård").appliedAmount(500.0).approvedAmount(500.0));
		when(lifecareFcIntegrationMock.getCalculations(PERSON_ID, "2026-01-01", "2026-06-30"))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO().addResultItem(dto));

		final var result = service.listCalculations(PERSON_ID, FROM, TO);

		assertThat(result).singleElement().satisfies(calculation -> {
			assertThat(calculation.id()).isEqualTo(7001);
			assertThat(calculation.norm()).isEqualTo("Riksnorm 2026");
			assertThat(calculation.normSum()).isEqualTo(10500.0);
			assertThat(calculation.isFinal()).isTrue();
			assertThat(calculation.persons()).singleElement().satisfies(person -> assertThat(person.personId()).isEqualTo("200001011234"));
			assertThat(calculation.incomes()).singleElement().satisfies(income -> assertThat(income.type()).isEqualTo("Lön"));
			assertThat(calculation.expenses()).singleElement().satisfies(expense -> assertThat(expense.approvedAmount()).isEqualTo(7000.0));
			assertThat(calculation.specialExpenses()).singleElement().satisfies(expense -> assertThat(expense.type()).isEqualTo("Tandvård"));
		});
	}

	@Test
	void listCalculationsEmptyWhenNoPage() {
		when(lifecareFcIntegrationMock.getCalculations(PERSON_ID, "2026-01-01", "2026-06-30")).thenReturn(null);

		assertThat(service.listCalculations(PERSON_ID, FROM, TO)).isEmpty();
	}

	@Test
	void listCalculationsHandlesNullRowLists() {
		when(lifecareFcIntegrationMock.getCalculations(PERSON_ID, "2026-01-01", "2026-06-30"))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO().addResultItem(new PersonBasedCalculationDTO().id(1)));

		assertThat(service.listCalculations(PERSON_ID, FROM, TO)).singleElement().satisfies(calculation -> {
			assertThat(calculation.persons()).isEmpty();
			assertThat(calculation.incomes()).isEmpty();
			assertThat(calculation.expenses()).isEmpty();
			assertThat(calculation.specialExpenses()).isEmpty();
		});
	}

	@Test
	void listDecisionsMapsHeaderAndPersons() {
		final var dto = new PersonBasedDecisionDTO()
			.id(9900).date("2026-06-02").type("Bifall").fromDate("2026-06-01").toDate("2026-06-30")
			.reason("Beviljas").decisionMaker("Anna").organization("IFO").amount(8500.0).coApplicant("198001019999").reasonCoApplicant("Sammanboende")
			.addDecisionPersonDTOsItem(new PersonBasedDecisionPersonDTO().personId("198001019999").name("Sven").isCoApplicant(true));
		when(lifecareFcIntegrationMock.getDecisions(PERSON_ID, "2026-01-01", "2026-06-30"))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO().addResultItem(dto));

		assertThat(service.listDecisions(PERSON_ID, FROM, TO)).singleElement().satisfies(decision -> {
			assertThat(decision.id()).isEqualTo(9900);
			assertThat(decision.type()).isEqualTo("Bifall");
			assertThat(decision.amount()).isEqualTo(8500.0);
			assertThat(decision.persons()).singleElement().satisfies(person -> assertThat(person.coApplicant()).isTrue());
		});
	}

	@Test
	void listDocumentsMapsMetadata() {
		final var dto = new PersonBasedDocumentDTO().id("doc-1").title("Beslut").date("2026-06-02").documentType("Beslut").ownerId("9900").ownerType("Decision");
		when(lifecareFcIntegrationMock.getDocuments(PERSON_ID, "2026-01-01", "2026-06-30"))
			.thenReturn(new ApiPaginationCompositePersonBasedDocumentDTO().addResultItem(dto));

		assertThat(service.listDocuments(PERSON_ID, FROM, TO)).singleElement().satisfies(document -> {
			assertThat(document.id()).isEqualTo("doc-1");
			assertThat(document.title()).isEqualTo("Beslut");
			assertThat(document.documentType()).isEqualTo("Beslut");
		});
	}

	@Test
	void documentContentForwards() {
		when(lifecareFcIntegrationMock.getDocumentContent("doc-1")).thenReturn("%PDF-1.4".getBytes());

		assertThat(service.documentContent("doc-1")).isEqualTo("%PDF-1.4".getBytes());
		verify(lifecareFcIntegrationMock).getDocumentContent("doc-1");
	}
}
