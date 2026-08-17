package se.sundsvall.caremanagement.lifecare.integration;

import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedCalculationDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedDecisionDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedExecutionDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedInvestigationDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedPaymentDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedResourceAllocationDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedServiceDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringProposalDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationProposalDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedContactDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedPersonDTO;
import generated.se.sundsvall.lifecarefamilycare.PostAktualiseringsBodyRequest;
import generated.se.sundsvall.lifecarefamilycare.PostCalculationBodyRequest;
import generated.se.sundsvall.lifecarefamilycare.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class LifecareFamilyCareIntegrationTest {

	private static final String PERSON_ID = "200001012384";
	private static final String START = "2026-04-01";
	private static final String END = "2026-06-30";

	@Mock
	private LifecareFamilyCareClient clientMock;

	@InjectMocks
	private LifecareFamilyCareIntegration integration;

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
		when(clientMock.getActualisations(PERSON_ID, START, END, null, null, false)).thenReturn(response);

		assertThat(integration.getActualisations(PERSON_ID, START, END)).isSameAs(response);
		verify(clientMock).getActualisations(PERSON_ID, START, END, null, null, false);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getCalculations() {
		final var response = new ApiPaginationCompositePersonBasedCalculationDTO();
		when(clientMock.getCalculations(PERSON_ID, START, END, null, null, false)).thenReturn(response);

		assertThat(integration.getCalculations(PERSON_ID, START, END)).isSameAs(response);
		verify(clientMock).getCalculations(PERSON_ID, START, END, null, null, false);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getDecisions() {
		final var response = new ApiPaginationCompositePersonBasedDecisionDTO();
		when(clientMock.getDecisions(PERSON_ID, START, END, null, null, false)).thenReturn(response);

		assertThat(integration.getDecisions(PERSON_ID, START, END)).isSameAs(response);
		verify(clientMock).getDecisions(PERSON_ID, START, END, null, null, false);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getPayments() {
		final var response = new ApiPaginationCompositePersonBasedPaymentDTO();
		when(clientMock.getPayments(PERSON_ID, START, END, null, null, false)).thenReturn(response);

		assertThat(integration.getPayments(PERSON_ID, START, END)).isSameAs(response);
		verify(clientMock).getPayments(PERSON_ID, START, END, null, null, false);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getInvestigations() {
		final var response = new ApiPaginationCompositePersonBasedInvestigationDTO();
		when(clientMock.getInvestigations(PERSON_ID, START, END, null, null, false)).thenReturn(response);

		assertThat(integration.getInvestigations(PERSON_ID, START, END)).isSameAs(response);
		verify(clientMock).getInvestigations(PERSON_ID, START, END, null, null, false);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getServices() {
		final var response = new ApiPaginationCompositePersonBasedServiceDTO();
		when(clientMock.getServices(PERSON_ID, START, END, null, null, false)).thenReturn(response);

		assertThat(integration.getServices(PERSON_ID, START, END)).isSameAs(response);
		verify(clientMock).getServices(PERSON_ID, START, END, null, null, false);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getExecutions() {
		final var response = new ApiPaginationCompositePersonBasedExecutionDTO();
		when(clientMock.getExecutions(PERSON_ID, START, END, null, null, false)).thenReturn(response);

		assertThat(integration.getExecutions(PERSON_ID, START, END)).isSameAs(response);
		verify(clientMock).getExecutions(PERSON_ID, START, END, null, null, false);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getUsers() {
		final var response = List.of(new User().id("9001").fullName("Anna Andersson").networkUserId("anna01ker"));
		when(clientMock.getUsers(1000, null, null, null)).thenReturn(response);

		assertThat(integration.getUsers(1000, null, null, null)).isSameAs(response);
		verify(clientMock).getUsers(1000, null, null, null);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getUsersFailureBecomesBadGateway() {
		when(clientMock.getUsers(1000, null, null, null)).thenThrow(Problem.valueOf(NOT_FOUND, "boom"));

		assertThatThrownBy(() -> integration.getUsers(1000, null, null, null))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.extracting(throwable -> ((ThrowableProblem) throwable).getDetail())
			.isEqualTo("Error fetching users in Lifecare FamilyCare: 404 Not Found: boom");
	}

	@Test
	void getResourceAllocations() {
		final var response = new ApiPaginationCompositePersonBasedResourceAllocationDTO();
		when(clientMock.getResourceAllocations(PERSON_ID, START, END, null, null, false)).thenReturn(response);

		assertThat(integration.getResourceAllocations(PERSON_ID, START, END)).isSameAs(response);
		verify(clientMock).getResourceAllocations(PERSON_ID, START, END, null, null, false);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getDecisionsFailure() {
		when(clientMock.getDecisions(PERSON_ID, START, END, null, null, false)).thenThrow(new RuntimeException("timeout"));

		assertThatThrownBy(() -> integration.getDecisions(PERSON_ID, START, END))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.extracting(throwable -> ((ThrowableProblem) throwable).getDetail())
			.isEqualTo("Error fetching decision in Lifecare FamilyCare: RuntimeException");

		verify(clientMock).getDecisions(PERSON_ID, START, END, null, null, false);
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
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.extracting(throwable -> ((ThrowableProblem) throwable).getDetail())
			.isEqualTo("Error fetching actualisation proposal in Lifecare FamilyCare: 404 Not Found: boom");

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
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.extracting(throwable -> ((ThrowableProblem) throwable).getDetail())
			.isEqualTo("Error creating actualisation in Lifecare FamilyCare: RuntimeException");

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
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.extracting(throwable -> ((ThrowableProblem) throwable).getDetail())
			.isEqualTo("Error creating calculation in Lifecare FamilyCare: 502 Bad Gateway: upstream down");

		verify(clientMock).createCalculation(body);
	}

	@Test
	void postActualisationAttachmentWrapsBytesAsTheContentPart() throws Exception {
		final var content = new byte[] {
			9, 8, 7
		};

		integration.postActualisationAttachment(4711, "DOC", "SENDER", "Title", "Sender", "EB-1_meddelandehistorik.pdf", content);

		final ArgumentCaptor<MultipartFile> fileCaptor = ArgumentCaptor.forClass(MultipartFile.class);
		verify(clientMock).postActualisationAttachment(eq(4711), eq("DOC"), eq("SENDER"), eq("Title"), eq("Sender"), fileCaptor.capture());
		final var file = fileCaptor.getValue();
		assertThat(file.getName()).isEqualTo("Content");
		assertThat(file.getOriginalFilename()).isEqualTo("EB-1_meddelandehistorik.pdf");
		assertThat(file.getContentType()).isEqualTo("application/pdf");
		assertThat(file.getBytes()).isEqualTo(content);
	}

	@Test
	void postActualisationAttachmentFailure() {
		doThrow(new RuntimeException("connection reset")).when(clientMock)
			.postActualisationAttachment(eq(4711), any(), any(), any(), any(), any());

		assertThatThrownBy(() -> integration.postActualisationAttachment(4711, "DOC", "SENDER", "Title", "Sender", "f.pdf", new byte[] {
			1
		}))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.extracting(throwable -> ((ThrowableProblem) throwable).getDetail())
			.isEqualTo("Error uploading actualisation attachment in Lifecare FamilyCare: RuntimeException");
	}

	// ---- describe(): transport-failure messages must not reach the problem detail ------------------------------------

	@Test
	void transportFailureMessageIsNotLeakedIntoProblemDetail() {
		// A Feign transport failure embeds the full request line (personId + key) in its message — must be dropped.
		final var leaky = "GET https://lifecare-familycare/Persons?personId=200001012384&key=SUPER-SECRET-KEY HTTP/1.1";
		when(clientMock.getPerson(PERSON_ID)).thenThrow(new RuntimeException(leaky));

		assertThatThrownBy(() -> integration.getPerson(PERSON_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.extracting(throwable -> ((ThrowableProblem) throwable).getDetail())
			.satisfies(detail -> {
				assertThat(detail).doesNotContain(leaky);
				assertThat(detail).doesNotContain("200001012384");
				assertThat(detail).doesNotContain("SUPER-SECRET-KEY");
				// Only the exception class name is exposed.
				assertThat(detail).isEqualTo("Error fetching person in Lifecare FamilyCare: RuntimeException");
			});
	}

	@Test
	void throwableProblemDetailIsStillExposed() {
		// ThrowableProblem causes are already clean — keep status + detail for self-diagnosing logs.
		when(clientMock.getPerson(PERSON_ID)).thenThrow(Problem.valueOf(NOT_FOUND, "person not found"));

		assertThatThrownBy(() -> integration.getPerson(PERSON_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.extracting(throwable -> ((ThrowableProblem) throwable).getDetail())
			.satisfies(detail -> assertThat(detail).contains("404").contains("person not found"));
	}
}
