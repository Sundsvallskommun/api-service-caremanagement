package se.sundsvall.caremanagement.lifecare.integration.integrator;

import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedContactDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedDecisionDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedPersonDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedServiceDTO;
import generated.se.sundsvall.lifecarefamilycare.User;
import generated.se.sundsvall.lifecareintegrator.Actualisation;
import generated.se.sundsvall.lifecareintegrator.Calculation;
import generated.se.sundsvall.lifecareintegrator.CalculationExpense;
import generated.se.sundsvall.lifecareintegrator.CalculationIncome;
import generated.se.sundsvall.lifecareintegrator.CalculationPerson;
import generated.se.sundsvall.lifecareintegrator.CaseService;
import generated.se.sundsvall.lifecareintegrator.Caseworker;
import generated.se.sundsvall.lifecareintegrator.Contact;
import generated.se.sundsvall.lifecareintegrator.Decision;
import generated.se.sundsvall.lifecareintegrator.DecisionsResponse;
import generated.se.sundsvall.lifecareintegrator.DocumentMetadata;
import generated.se.sundsvall.lifecareintegrator.PagedActualisationResponse;
import generated.se.sundsvall.lifecareintegrator.PagedCalculationResponse;
import generated.se.sundsvall.lifecareintegrator.PagedDocumentResponse;
import generated.se.sundsvall.lifecareintegrator.PagedPaymentResponse;
import generated.se.sundsvall.lifecareintegrator.PagedServiceResponse;
import generated.se.sundsvall.lifecareintegrator.PagingMetaData;
import generated.se.sundsvall.lifecareintegrator.Payment;
import generated.se.sundsvall.lifecareintegrator.Person;
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

import static generated.se.sundsvall.lifecareintegrator.Decision.SourceEnum.FAMILY_CARE;
import static java.nio.charset.StandardCharsets.UTF_8;
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

	// ---- The remaining ported reads ---------------------------------------------------------------------------------
	//
	// The field-by-field translation is IntegratorCaseMapperTest's job; what matters here is that each operation
	// resolves the person to a party id first and hands the mapped result back.

	@Test
	void getDecisionsGoesThroughTheIntegrator() {
		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, PERSON_ID)).thenReturn(Optional.of(PARTY_ID));
		when(clientMock.getDecisions(MUNICIPALITY_ID, PARTY_ID, START, END)).thenReturn(new DecisionsResponse()
			.decisions(List.of(new Decision().source(FAMILY_CARE).decisionId("4711"))));

		assertThat(integration.getDecisions(MUNICIPALITY_ID, PERSON_ID, START, END).getResult())
			.singleElement().extracting(PersonBasedDecisionDTO::getId).isEqualTo(4711);
		verify(clientMock).getDecisions(MUNICIPALITY_ID, PARTY_ID, START, END);
	}

	@Test
	void getActualisationsGoesThroughTheIntegrator() {
		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, PERSON_ID)).thenReturn(Optional.of(PARTY_ID));
		when(clientMock.getActualisations(MUNICIPALITY_ID, PARTY_ID, START, END)).thenReturn(new PagedActualisationResponse()
			.actualisations(List.of(new Actualisation().id(12))));

		assertThat(integration.getActualisations(MUNICIPALITY_ID, PERSON_ID, START, END).getResult())
			.singleElement().extracting(PersonBasedAktualiseringDTO::getPersonId).isEqualTo(PARTY_ID);
		verify(clientMock).getActualisations(MUNICIPALITY_ID, PARTY_ID, START, END);
	}

	@Test
	void getPaymentsGoesThroughTheIntegrator() {
		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, PERSON_ID)).thenReturn(Optional.of(PARTY_ID));
		when(clientMock.getPayments(MUNICIPALITY_ID, PARTY_ID, START, END)).thenReturn(new PagedPaymentResponse()
			.payments(List.of(new Payment().id(9))));

		assertThat(integration.getPayments(MUNICIPALITY_ID, PERSON_ID, START, END).getResult()).hasSize(1);
		verify(clientMock).getPayments(MUNICIPALITY_ID, PARTY_ID, START, END);
	}

	@Test
	void getServicesGoesThroughTheIntegrator() {
		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, PERSON_ID)).thenReturn(Optional.of(PARTY_ID));
		when(clientMock.getServices(MUNICIPALITY_ID, PARTY_ID, START, END)).thenReturn(new PagedServiceResponse()
			.services(List.of(new CaseService().id(3).caseworker("Karin Karlsson"))));

		assertThat(integration.getServices(MUNICIPALITY_ID, PERSON_ID, START, END).getResult())
			.singleElement().extracting(PersonBasedServiceDTO::getCaseworker).isEqualTo("Karin Karlsson");
		verify(clientMock).getServices(MUNICIPALITY_ID, PARTY_ID, START, END);
	}

	@Test
	void getDocumentsGoesThroughTheIntegrator() {
		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, PERSON_ID)).thenReturn(Optional.of(PARTY_ID));
		when(clientMock.getDocuments(MUNICIPALITY_ID, PARTY_ID, START, END)).thenReturn(new PagedDocumentResponse()
			.documents(List.of(new DocumentMetadata().id("doc-1"))));

		assertThat(integration.getDocuments(MUNICIPALITY_ID, PERSON_ID, START, END).getResult()).hasSize(1);
		verify(clientMock).getDocuments(MUNICIPALITY_ID, PARTY_ID, START, END);
	}

	/** Document content is fetched by document id, so there is no person to resolve. */
	@Test
	void getDocumentContentNeedsNoPartyId() {
		final var content = "%PDF-1.7".getBytes(UTF_8);
		when(clientMock.getDocumentContent(MUNICIPALITY_ID, "doc-1")).thenReturn(content);

		assertThat(integration.getDocumentContent(MUNICIPALITY_ID, "doc-1")).isEqualTo(content);
		verifyNoInteractions(citizenServiceMock);
	}

	@Test
	void getPersonGoesThroughTheIntegrator() {
		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, PERSON_ID)).thenReturn(Optional.of(PARTY_ID));
		when(clientMock.getPerson(MUNICIPALITY_ID, PARTY_ID)).thenReturn(new Person().name("Berit Berg").addressProtection(true));

		assertThat(integration.getPerson(MUNICIPALITY_ID, PERSON_ID))
			.returns(PARTY_ID, PersonBasedPersonDTO::getPersonId)
			.returns("Berit Berg", PersonBasedPersonDTO::getName)
			.returns(true, PersonBasedPersonDTO::getAddressProtection);
	}

	@Test
	void getContactsGoesThroughTheIntegrator() {
		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, PERSON_ID)).thenReturn(Optional.of(PARTY_ID));
		when(clientMock.getContacts(MUNICIPALITY_ID, PARTY_ID)).thenReturn(List.of(new Contact().name("Anna Andersson")));

		assertThat(integration.getContacts(MUNICIPALITY_ID, PERSON_ID))
			.singleElement().extracting(PersonBasedContactDTO::getName).isEqualTo("Anna Andersson");
	}

	/** The caseworker directory is the one read with no person behind it. */
	@Test
	void getUsersNeedsNoPartyId() {
		when(clientMock.getUsers(MUNICIPALITY_ID, 100, null, null, null)).thenReturn(List.of(new Caseworker().fullName("Karin Karlsson")));

		assertThat(integration.getUsers(MUNICIPALITY_ID, 100, null, null, null))
			.singleElement().extracting(User::getFullName).isEqualTo("Karin Karlsson");
		verifyNoInteractions(citizenServiceMock);
	}

	/**
	 * An operation that is not translated yet has to fail loudly. An empty result would be indistinguishable from "this
	 * person has nothing", and a handläggare would be shown a normberäkning built on a silent gap.
	 */
	@Test
	void unportedOperationsFailInsteadOfAnsweringEmpty() {
		assertThatThrownBy(() -> integration.getCalculationProposal(MUNICIPALITY_ID, PERSON_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_IMPLEMENTED)
			.hasMessageContaining("getCalculationProposal");

		assertThatThrownBy(() -> integration.getInvestigations(MUNICIPALITY_ID, PERSON_ID, START, END)).isInstanceOf(ThrowableProblem.class);
		assertThatThrownBy(() -> integration.getExecutions(MUNICIPALITY_ID, PERSON_ID, START, END)).isInstanceOf(ThrowableProblem.class);
		assertThatThrownBy(() -> integration.getResourceAllocations(MUNICIPALITY_ID, PERSON_ID, START, END)).isInstanceOf(ThrowableProblem.class);
		assertThatThrownBy(() -> integration.getActualisationProposal(MUNICIPALITY_ID, PERSON_ID)).isInstanceOf(ThrowableProblem.class);
		assertThatThrownBy(() -> integration.createActualisation(MUNICIPALITY_ID, null)).isInstanceOf(ThrowableProblem.class);
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
