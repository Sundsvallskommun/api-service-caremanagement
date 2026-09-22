package se.sundsvall.caremanagement.lifecare.integration.integrator;

import generated.se.sundsvall.lifecareintegrator.Calculation;
import generated.se.sundsvall.lifecareintegrator.CalculationExpense;
import generated.se.sundsvall.lifecareintegrator.CalculationIncome;
import generated.se.sundsvall.lifecareintegrator.CalculationPerson;
import generated.se.sundsvall.lifecareintegrator.PagedCalculationResponse;
import generated.se.sundsvall.lifecareintegrator.PagingMetaData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.time.Month.APRIL;
import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;

@ExtendWith(MockitoExtension.class)
class LifecareIntegratorIntegrationTest {

	private static final String MUNICIPALITY_ID = "2281";

	private static final String PERSON_ID = "200001012384";
	private static final String PARTY_ID = "6a5c3d18-1f2b-4e77-9c0a-2b3d4e5f6a7b";
	private static final LocalDate START = LocalDate.of(2026, APRIL, 1);
	private static final LocalDate END = LocalDate.of(2026, JUNE, 30);

	@Mock
	private LifecareIntegratorClient clientMock;

	@Mock
	private CitizenService citizenServiceMock;

	@InjectMocks
	private LifecareIntegratorIntegration integration;

	@Test
	void getCalculationsResolvesThePersonToAPartyIdBeforeCalling() {
		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, PERSON_ID)).thenReturn(Optional.of(PARTY_ID));
		when(clientMock.getCalculations(MUNICIPALITY_ID, PARTY_ID, START, END)).thenReturn(new PagedCalculationResponse());

		integration.getCalculations(MUNICIPALITY_ID, PERSON_ID, START, END);

		verify(citizenServiceMock).getPartyId(MUNICIPALITY_ID, PERSON_ID);
		verify(clientMock).getCalculations(MUNICIPALITY_ID, PARTY_ID, START, END);
		verifyNoMoreInteractions(clientMock, citizenServiceMock);
	}

	/**
	 * The household roster is the reason this route exists at all, so the person mapping earns its own assertion: the
	 * party id has to land in {@code personId}, which is the field the lifecare services filter on. Dropping it empties
	 * the roster without an error.
	 */
	@Test
	void getCalculationsMapsTheIntegratorsModelBackToFamilyCare() {
		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, PERSON_ID)).thenReturn(Optional.of(PARTY_ID));
		when(clientMock.getCalculations(MUNICIPALITY_ID, PARTY_ID, START, END)).thenReturn(new PagedCalculationResponse()
			.meta(new PagingMetaData().page(1).limit(20).totalPages(3).totalRecords(42L))
			.calculations(List.of(new Calculation()
				.id(5012)
				.norm("Riksnorm 2026")
				.fromDate(LocalDate.of(2026, JUNE, 1))
				.toDate(LocalDate.of(2026, JUNE, 30))
				.normSum(BigDecimal.valueOf(12345.0))
				.incomeSum(BigDecimal.valueOf(8000.0))
				.totalSum(BigDecimal.valueOf(-2155.0))
				.investigationId(77)
				.serviceId(88)
				.connectedApplication(99)
				.finalCalculation(true)
				.persons(List.of(new CalculationPerson()
					.partyId(PARTY_ID)
					.name("Berit Berg")
					.amount(BigDecimal.valueOf(3160.0))
					.deviationFromDate(LocalDate.of(2026, JUNE, 10))))
				.incomes(List.of(new CalculationIncome().type("Lön efter skatt").amountApplicant(BigDecimal.valueOf(8000.0))))
				.expenses(List.of(new CalculationExpense().type("Hyra").appliedAmount(BigDecimal.valueOf(6500.0))))
				.specialExpenses(List.of(new CalculationExpense().type("Tandvård").approvedAmount(BigDecimal.valueOf(450.0)))))));

		final var result = integration.getCalculations(MUNICIPALITY_ID, PERSON_ID, START, END);

		assertThat(result.getPageNumber()).isEqualTo(1);
		assertThat(result.getPageSize()).isEqualTo(20);
		assertThat(result.getTotalNumberOfPages()).isEqualTo(3);
		assertThat(result.getTotalNumberOfRecords()).isEqualTo(42);
		assertThat(result.getResult()).singleElement().satisfies(calculation -> {
			assertThat(calculation.getId()).isEqualTo(5012);
			assertThat(calculation.getNorm()).isEqualTo("Riksnorm 2026");
			assertThat(calculation.getFromDate()).isEqualTo("2026-06-01");
			assertThat(calculation.getToDate()).isEqualTo("2026-06-30");
			assertThat(calculation.getNormSum()).isEqualTo(12345.0);
			assertThat(calculation.getIncomeSum()).isEqualTo(8000.0);
			assertThat(calculation.getTotalSum()).isEqualTo(-2155.0);
			assertThat(calculation.getExpenseSum()).isNull();
			assertThat(calculation.getInvestigationId()).isEqualTo(77);
			assertThat(calculation.getServiceId()).isEqualTo(88);
			assertThat(calculation.getConnectedApplication()).isEqualTo(99);
			assertThat(calculation.getFinal()).isTrue();
			assertThat(calculation.getCalculationPersonDTOs()).singleElement().satisfies(person -> {
				assertThat(person.getPersonId()).isEqualTo(PARTY_ID);
				assertThat(person.getName()).isEqualTo("Berit Berg");
				assertThat(person.getAmount()).isEqualTo(3160.0);
				assertThat(person.getDeviationFromDate()).isEqualTo("2026-06-10");
				assertThat(person.getDeviationToDate()).isNull();
			});
			assertThat(calculation.getCalculationIncomesDTOs()).singleElement().satisfies(income -> {
				assertThat(income.getType()).isEqualTo("Lön efter skatt");
				assertThat(income.getAmountApplicant()).isEqualTo(8000.0);
				assertThat(income.getAmountCoApplicant()).isNull();
			});
			assertThat(calculation.getCalculationExpensesDTOs()).singleElement().satisfies(expense -> {
				assertThat(expense.getType()).isEqualTo("Hyra");
				assertThat(expense.getAppliedAmount()).isEqualTo(6500.0);
			});
			assertThat(calculation.getCalculationSpecialExpensesDTOs()).singleElement().satisfies(expense -> {
				assertThat(expense.getType()).isEqualTo("Tandvård");
				assertThat(expense.getApprovedAmount()).isEqualTo(450.0);
			});
		});
	}

	@Test
	void getCalculationsWithNoResponseYieldsAnEmptyResultRatherThanNull() {
		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, PERSON_ID)).thenReturn(Optional.of(PARTY_ID));
		when(clientMock.getCalculations(MUNICIPALITY_ID, PARTY_ID, START, END)).thenReturn(null);

		final var result = integration.getCalculations(MUNICIPALITY_ID, PERSON_ID, START, END);

		assertThat(result.getResult()).isEmpty();
		assertThat(result.getPageNumber()).isNull();
	}

	@Test
	void getCalculationsWithoutAResolvablePartyIdFailsBeforeCallingTheIntegrator() {
		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, PERSON_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> integration.getCalculations(MUNICIPALITY_ID, PERSON_ID, START, END))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.hasMessageContaining("No party id");

		verify(clientMock, never()).getCalculations(any(), any(), any(), any());
	}

	/**
	 * The failure message deliberately says only what class of failure it was. A Feign exception embeds the request
	 * line, which on this route carries the party id and the query window.
	 */
	@Test
	void getCalculationsTranslatesAnUpstreamFailureIntoBadGateway() {
		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, PERSON_ID)).thenReturn(Optional.of(PARTY_ID));
		when(clientMock.getCalculations(MUNICIPALITY_ID, PARTY_ID, START, END)).thenThrow(new IllegalStateException("GET /2281/calculations?partyId=" + PARTY_ID));

		assertThatThrownBy(() -> integration.getCalculations(MUNICIPALITY_ID, PERSON_ID, START, END))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.hasMessageContaining("fetching calculations")
			.hasMessageContaining("IllegalStateException")
			.hasMessageNotContaining(PARTY_ID);
	}

	/**
	 * An operation that is not translated yet has to fail loudly. An empty result would be indistinguishable from "this
	 * person has nothing", and a handläggare would be shown a normberäkning built on a silent gap.
	 */
	@Test
	void unportedOperationsFailInsteadOfAnsweringEmpty() {
		assertThatThrownBy(() -> integration.getDecisions(MUNICIPALITY_ID, PERSON_ID, START, END))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_IMPLEMENTED)
			.hasMessageContaining("getDecisions");

		assertThatThrownBy(() -> integration.getPerson(MUNICIPALITY_ID, PERSON_ID)).isInstanceOf(ThrowableProblem.class);
		assertThatThrownBy(() -> integration.getContacts(MUNICIPALITY_ID, PERSON_ID)).isInstanceOf(ThrowableProblem.class);
		assertThatThrownBy(() -> integration.getActualisations(MUNICIPALITY_ID, PERSON_ID, START, END)).isInstanceOf(ThrowableProblem.class);
		assertThatThrownBy(() -> integration.getPayments(MUNICIPALITY_ID, PERSON_ID, START, END)).isInstanceOf(ThrowableProblem.class);
		assertThatThrownBy(() -> integration.getInvestigations(MUNICIPALITY_ID, PERSON_ID, START, END)).isInstanceOf(ThrowableProblem.class);
		assertThatThrownBy(() -> integration.getServices(MUNICIPALITY_ID, PERSON_ID, START, END)).isInstanceOf(ThrowableProblem.class);
		assertThatThrownBy(() -> integration.getExecutions(MUNICIPALITY_ID, PERSON_ID, START, END)).isInstanceOf(ThrowableProblem.class);
		assertThatThrownBy(() -> integration.getResourceAllocations(MUNICIPALITY_ID, PERSON_ID, START, END)).isInstanceOf(ThrowableProblem.class);
		assertThatThrownBy(() -> integration.getUsers(MUNICIPALITY_ID, 100, 0, null, null)).isInstanceOf(ThrowableProblem.class);
		assertThatThrownBy(() -> integration.getDocuments(MUNICIPALITY_ID, PERSON_ID, START, END)).isInstanceOf(ThrowableProblem.class);
		assertThatThrownBy(() -> integration.getDocumentContent(MUNICIPALITY_ID, "some-document-id")).isInstanceOf(ThrowableProblem.class);
		assertThatThrownBy(() -> integration.getActualisationProposal(MUNICIPALITY_ID, PERSON_ID)).isInstanceOf(ThrowableProblem.class);
		assertThatThrownBy(() -> integration.createActualisation(MUNICIPALITY_ID, null)).isInstanceOf(ThrowableProblem.class);
		assertThatThrownBy(() -> integration.getCalculationProposal(MUNICIPALITY_ID, PERSON_ID)).isInstanceOf(ThrowableProblem.class);
		assertThatThrownBy(() -> integration.createCalculation(MUNICIPALITY_ID, null)).isInstanceOf(ThrowableProblem.class);
		assertThatThrownBy(() -> integration.postActualisationAttachment(MUNICIPALITY_ID, 1, "type", "senderType", "title", "sender", "file.pdf", new byte[0]))
			.isInstanceOf(ThrowableProblem.class);

		verifyNoInteractions(clientMock, citizenServiceMock);
	}

	/**
	 * The direct FamilyCare client stays the default; this route only takes over when the property explicitly selects
	 * it. Without {@code matchIfMissing = false} both implementations would be beans and the context would not start.
	 */
	@Test
	void theRouteIsOptInOnly() {
		final var condition = LifecareIntegratorIntegration.class.getAnnotation(ConditionalOnProperty.class);

		assertThat(condition).isNotNull();
		assertThat(condition.name()).containsExactly("integration.lifecare-familycare.provider");
		assertThat(condition.havingValue()).isEqualTo("integrator");
		assertThat(condition.matchIfMissing()).isFalse();
	}
}
