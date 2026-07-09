package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseHistoryService;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationExpenseView;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationIncomeView;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationPersonView;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationView;
import se.sundsvall.caremanagement.lifecare.service.model.DecisionPersonView;
import se.sundsvall.caremanagement.lifecare.service.model.DecisionView;
import se.sundsvall.caremanagement.lifecare.service.model.DocumentView;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.time.Month.JANUARY;
import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class FinancialAssistanceLifecareServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String APPLICANT_PARTY_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";

	@Mock
	private CitizenService citizenServiceMock;

	@Mock
	private LifecareCaseHistoryService lifecareCaseHistoryServiceMock;

	@InjectMocks
	private FinancialAssistanceLifecareService service;

	@Test
	void listCalculationsResolvesPartyDefaultsPeriodAndMaps() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		final var view = new CalculationView(7001, "Riksnorm 2026", "2026-06-01", "2026-06-30", 12000.0, 9500.0, 500.0, 10500.0,
			1200.0, 800.0, -2000.0, 8500.0, Boolean.TRUE,
			List.of(new CalculationPersonView("200001011234", "Barn Andersson", 4500.0, null, null)),
			List.of(new CalculationIncomeView("Lön", 12000.0, "2026-05-15", 0.0, null)),
			List.of(new CalculationExpenseView("Hyra", 7500.0, 7000.0)),
			List.of(new CalculationExpenseView("Tandvård", 500.0, 500.0)));
		when(lifecareCaseHistoryServiceMock.listCalculations(eq("199001011234"), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of(view));

		final var result = service.listCalculations(MUNICIPALITY_ID, APPLICANT_PARTY_ID, null, null);

		assertThat(result).singleElement().satisfies(calculation -> {
			assertThat(calculation.getId()).isEqualTo(7001);
			assertThat(calculation.getNormSum()).isEqualTo(10500.0);
			assertThat(calculation.getIsFinal()).isTrue();
			assertThat(calculation.getPersons()).singleElement().satisfies(person -> assertThat(person.getPersonId()).isEqualTo("200001011234"));
			assertThat(calculation.getIncomes()).singleElement().satisfies(income -> assertThat(income.getType()).isEqualTo("Lön"));
			assertThat(calculation.getExpenses()).singleElement().satisfies(expense -> assertThat(expense.getApprovedAmount()).isEqualTo(7000.0));
			assertThat(calculation.getSpecialExpenses()).singleElement().satisfies(expense -> assertThat(expense.getType()).isEqualTo("Tandvård"));
		});

		final var fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
		final var toCaptor = ArgumentCaptor.forClass(LocalDate.class);
		verify(lifecareCaseHistoryServiceMock).listCalculations(eq("199001011234"), fromCaptor.capture(), toCaptor.capture());
		assertThat(toCaptor.getValue()).isEqualTo(LocalDate.now());
		assertThat(fromCaptor.getValue()).isEqualTo(toCaptor.getValue().minusMonths(24));
	}

	@Test
	void listCalculationsUnresolvedPartyIdYields404() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.listCalculations(MUNICIPALITY_ID, APPLICANT_PARTY_ID, null, null))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: No citizen found for partyId f47ac10b-58cc-4372-a567-0e02b2c3d479");

		verify(lifecareCaseHistoryServiceMock, never()).listCalculations(any(), any(), any());
	}

	@Test
	void listDecisionsResolvesPartyDefaultsPeriodAndMaps() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		final var view = new DecisionView(9900, "2026-06-02", "Bifall", "2026-06-01", "2026-06-30", "Beviljas enligt norm",
			"Anna Andersson", "IFO", 8500.0, "198001019999", "Sammanboende",
			List.of(new DecisionPersonView("198001019999", "Sven Svensson", Boolean.TRUE)));
		when(lifecareCaseHistoryServiceMock.listDecisions(eq("199001011234"), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of(view));

		final var result = service.listDecisions(MUNICIPALITY_ID, APPLICANT_PARTY_ID, LocalDate.of(2026, JANUARY, 1), LocalDate.of(2026, JUNE, 30));

		assertThat(result).singleElement().satisfies(decision -> {
			assertThat(decision.getId()).isEqualTo(9900);
			assertThat(decision.getType()).isEqualTo("Bifall");
			assertThat(decision.getAmount()).isEqualTo(8500.0);
			assertThat(decision.getPersons()).singleElement().satisfies(person -> assertThat(person.getCoApplicant()).isTrue());
		});
		verify(lifecareCaseHistoryServiceMock).listDecisions("199001011234", LocalDate.of(2026, JANUARY, 1), LocalDate.of(2026, JUNE, 30));
	}

	@Test
	void listDocumentsResolvesPartyAndMaps() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		final var view = new DocumentView("doc-1", "Beslut försörjningsstöd", "2026-06-02", "Beslut", "9900", "Decision");
		when(lifecareCaseHistoryServiceMock.listDocuments(eq("199001011234"), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of(view));

		final var result = service.listDocuments(MUNICIPALITY_ID, APPLICANT_PARTY_ID, null, null);

		assertThat(result).singleElement().satisfies(document -> {
			assertThat(document.getId()).isEqualTo("doc-1");
			assertThat(document.getTitle()).isEqualTo("Beslut försörjningsstöd");
			assertThat(document.getDocumentType()).isEqualTo("Beslut");
		});
		verify(lifecareCaseHistoryServiceMock).listDocuments(eq("199001011234"), any(LocalDate.class), any(LocalDate.class));
	}

	@Test
	void readDocumentContentForwardsBytesWhenOwnedByApplicant() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(lifecareCaseHistoryServiceMock.listDocuments(eq("199001011234"), any(LocalDate.class), any(LocalDate.class)))
			.thenReturn(List.of(new DocumentView("doc-1", "Beslut", "2026-06-02", "Beslut", "9900", "Decision")));
		when(lifecareCaseHistoryServiceMock.documentContent("doc-1")).thenReturn("%PDF-1.4".getBytes());

		final var content = service.readDocumentContent(MUNICIPALITY_ID, APPLICANT_PARTY_ID, "doc-1", null, null);

		assertThat(content).isEqualTo("%PDF-1.4".getBytes());
		verify(lifecareCaseHistoryServiceMock).documentContent("doc-1");
	}

	@Test
	void readDocumentContentForeignDocumentYields404() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(lifecareCaseHistoryServiceMock.listDocuments(eq("199001011234"), any(LocalDate.class), any(LocalDate.class)))
			.thenReturn(List.of(new DocumentView("doc-1", "Beslut", "2026-06-02", "Beslut", "9900", "Decision")));

		assertThatThrownBy(() -> service.readDocumentContent(MUNICIPALITY_ID, APPLICANT_PARTY_ID, "doc-OTHER", null, null))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: No Lifecare document 'doc-OTHER' found for the given applicant");

		verify(lifecareCaseHistoryServiceMock, never()).documentContent(any());
	}
}
