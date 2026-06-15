package se.sundsvall.caremanagement.lifecare.integration;

import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedCalculationDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedDecisionDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedExecutionDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedInvestigationDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedPaymentDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedResourceAllocationDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedServiceDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedAktualiseringProposalDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationProposalDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedContactDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedPersonDTO;
import generated.se.sundsvall.lifecarefc.PostAktualiseringsBodyRequest;
import generated.se.sundsvall.lifecarefc.PostCalculationBodyRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class LifecareFcIntegrationTest {

	private static final String PERSON_ID = "200001012384";
	private static final String START = "2026-04-01";
	private static final String END = "2026-06-30";
	private static final Integer PAGE_SIZE = 50;
	private static final Integer PAGE_NR = 0;
	private static final Boolean ASCENDING = true;

	@Mock
	private LifecareFcClient clientMock;

	@InjectMocks
	private LifecareFcIntegration integration;

	// ---- Person-based reads ------------------------------------------------------------------------------------------

	@Test
	void getPerson() {
		final var response = new PersonBasedPersonDTO();
		when(clientMock.getPerson(PERSON_ID)).thenReturn(response);

		assertThat(integration.getPerson(PERSON_ID)).isSameAs(response);
		verify(clientMock).getPerson(PERSON_ID);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getContacts() {
		final var response = List.of(new PersonBasedContactDTO());
		when(clientMock.getContacts(PERSON_ID)).thenReturn(response);

		assertThat(integration.getContacts(PERSON_ID)).isSameAs(response);
		verify(clientMock).getContacts(PERSON_ID);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getActualisations() {
		final var response = new ApiPaginationCompositePersonBasedAktualiseringDTO();
		when(clientMock.getActualisations(PERSON_ID, START, END, PAGE_SIZE, PAGE_NR, ASCENDING)).thenReturn(response);

		assertThat(integration.getActualisations(PERSON_ID, START, END, PAGE_SIZE, PAGE_NR, ASCENDING)).isSameAs(response);
		verify(clientMock).getActualisations(PERSON_ID, START, END, PAGE_SIZE, PAGE_NR, ASCENDING);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getCalculations() {
		final var response = new ApiPaginationCompositePersonBasedCalculationDTO();
		when(clientMock.getCalculations(PERSON_ID, START, END, PAGE_SIZE, PAGE_NR, ASCENDING)).thenReturn(response);

		assertThat(integration.getCalculations(PERSON_ID, START, END, PAGE_SIZE, PAGE_NR, ASCENDING)).isSameAs(response);
		verify(clientMock).getCalculations(PERSON_ID, START, END, PAGE_SIZE, PAGE_NR, ASCENDING);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getDecisions() {
		final var response = new ApiPaginationCompositePersonBasedDecisionDTO();
		when(clientMock.getDecisions(PERSON_ID, START, END, PAGE_SIZE, PAGE_NR, ASCENDING)).thenReturn(response);

		assertThat(integration.getDecisions(PERSON_ID, START, END, PAGE_SIZE, PAGE_NR, ASCENDING)).isSameAs(response);
		verify(clientMock).getDecisions(PERSON_ID, START, END, PAGE_SIZE, PAGE_NR, ASCENDING);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getPayments() {
		final var response = new ApiPaginationCompositePersonBasedPaymentDTO();
		when(clientMock.getPayments(PERSON_ID, START, END, PAGE_SIZE, PAGE_NR, ASCENDING)).thenReturn(response);

		assertThat(integration.getPayments(PERSON_ID, START, END, PAGE_SIZE, PAGE_NR, ASCENDING)).isSameAs(response);
		verify(clientMock).getPayments(PERSON_ID, START, END, PAGE_SIZE, PAGE_NR, ASCENDING);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getInvestigations() {
		final var response = new ApiPaginationCompositePersonBasedInvestigationDTO();
		when(clientMock.getInvestigations(PERSON_ID, START, END, PAGE_SIZE, PAGE_NR, ASCENDING)).thenReturn(response);

		assertThat(integration.getInvestigations(PERSON_ID, START, END, PAGE_SIZE, PAGE_NR, ASCENDING)).isSameAs(response);
		verify(clientMock).getInvestigations(PERSON_ID, START, END, PAGE_SIZE, PAGE_NR, ASCENDING);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getServices() {
		final var response = new ApiPaginationCompositePersonBasedServiceDTO();
		when(clientMock.getServices(PERSON_ID, START, END, PAGE_SIZE, PAGE_NR, ASCENDING)).thenReturn(response);

		assertThat(integration.getServices(PERSON_ID, START, END, PAGE_SIZE, PAGE_NR, ASCENDING)).isSameAs(response);
		verify(clientMock).getServices(PERSON_ID, START, END, PAGE_SIZE, PAGE_NR, ASCENDING);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getExecutions() {
		final var response = new ApiPaginationCompositePersonBasedExecutionDTO();
		when(clientMock.getExecutions(PERSON_ID, START, END, PAGE_SIZE, PAGE_NR, ASCENDING)).thenReturn(response);

		assertThat(integration.getExecutions(PERSON_ID, START, END, PAGE_SIZE, PAGE_NR, ASCENDING)).isSameAs(response);
		verify(clientMock).getExecutions(PERSON_ID, START, END, PAGE_SIZE, PAGE_NR, ASCENDING);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getResourceAllocations() {
		final var response = new ApiPaginationCompositePersonBasedResourceAllocationDTO();
		when(clientMock.getResourceAllocations(PERSON_ID, START, END, PAGE_SIZE, PAGE_NR, ASCENDING)).thenReturn(response);

		assertThat(integration.getResourceAllocations(PERSON_ID, START, END, PAGE_SIZE, PAGE_NR, ASCENDING)).isSameAs(response);
		verify(clientMock).getResourceAllocations(PERSON_ID, START, END, PAGE_SIZE, PAGE_NR, ASCENDING);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getDecisionsFailure() {
		when(clientMock.getDecisions(PERSON_ID, START, END, PAGE_SIZE, PAGE_NR, ASCENDING)).thenThrow(new RuntimeException("timeout"));

		assertThatThrownBy(() -> integration.getDecisions(PERSON_ID, START, END, PAGE_SIZE, PAGE_NR, ASCENDING))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY);

		verify(clientMock).getDecisions(PERSON_ID, START, END, PAGE_SIZE, PAGE_NR, ASCENDING);
	}

	// ---- Write-back + proposals --------------------------------------------------------------------------------------

	@Test
	void getActualisationProposal() {
		final var response = new PersonBasedAktualiseringProposalDTO();
		when(clientMock.getActualisationProposal(PERSON_ID)).thenReturn(response);

		assertThat(integration.getActualisationProposal(PERSON_ID)).isSameAs(response);
		verify(clientMock).getActualisationProposal(PERSON_ID);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getActualisationProposalFailure() {
		when(clientMock.getActualisationProposal(PERSON_ID)).thenThrow(Problem.valueOf(NOT_FOUND, "boom"));

		assertThatThrownBy(() -> integration.getActualisationProposal(PERSON_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY);

		verify(clientMock).getActualisationProposal(PERSON_ID);
	}

	@Test
	void createActualisation() {
		final var body = new PostAktualiseringsBodyRequest();
		when(clientMock.createActualisation(body)).thenReturn(4711);

		assertThat(integration.createActualisation(body)).isEqualTo(4711);
		verify(clientMock).createActualisation(body);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void createActualisationFailure() {
		final var body = new PostAktualiseringsBodyRequest();
		when(clientMock.createActualisation(body)).thenThrow(new RuntimeException("connection reset"));

		assertThatThrownBy(() -> integration.createActualisation(body))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY);

		verify(clientMock).createActualisation(body);
	}

	@Test
	void getCalculationProposal() {
		final var response = new PersonBasedCalculationProposalDTO();
		when(clientMock.getCalculationProposal(PERSON_ID)).thenReturn(response);

		assertThat(integration.getCalculationProposal(PERSON_ID)).isSameAs(response);
		verify(clientMock).getCalculationProposal(PERSON_ID);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void createCalculation() {
		final var body = new PostCalculationBodyRequest();
		when(clientMock.createCalculation(body)).thenReturn(99);

		assertThat(integration.createCalculation(body)).isEqualTo(99);
		verify(clientMock).createCalculation(body);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void createCalculationFailure() {
		final var body = new PostCalculationBodyRequest();
		when(clientMock.createCalculation(body)).thenThrow(Problem.valueOf(BAD_GATEWAY, "upstream down"));

		assertThatThrownBy(() -> integration.createCalculation(body))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY);

		verify(clientMock).createCalculation(body);
	}
}
