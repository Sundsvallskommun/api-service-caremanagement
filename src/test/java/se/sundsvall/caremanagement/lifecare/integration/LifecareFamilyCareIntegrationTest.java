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
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationPersonPostDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationProposalDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedContactDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedPersonDTO;
import generated.se.sundsvall.lifecarefamilycare.PostAktualiseringsBodyRequest;
import generated.se.sundsvall.lifecarefamilycare.PostCalculationBodyRequest;
import generated.se.sundsvall.lifecarefamilycare.User;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.time.Month.APRIL;
import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class LifecareFamilyCareIntegrationTest {

	private static final String MUNICIPALITY_ID = "2281";

	private static final String PERSON_ID = "200001012384";
	private static final String CHILD_PERSON_ID = "201801012380";
	private static final String APPLICANT_PARTY_ID = "6a5c3d18-1f3b-4c2a-9d9e-2b7f4a1c8e55";
	private static final String CHILD_PARTY_ID = "f0c9b8a7-6d5e-4c3b-8a19-0e7d6c5b4a32";
	private static final LocalDate START = LocalDate.of(2026, APRIL, 1);
	private static final LocalDate END = LocalDate.of(2026, JUNE, 30);

	// The same window on the wire: FamilyCare requires RFC 3339 with a time component, and the window is inclusive in
	// both ends, so the end date is sent as end of day.
	private static final String START_WIRE = "2026-04-01T00:00:00";
	private static final String END_WIRE = "2026-06-30T23:59:59";

	@Mock
	private LifecareFamilyCareClient clientMock;

	@Mock
	private CitizenService citizenServiceMock;

	@InjectMocks
	private LifecareFamilyCareIntegration integration;

	/** Arguments are party ids; this route resolves them to the personal identity number FamilyCare keys on. */
	@BeforeEach
	void resolveApplicant() {
		lenient().when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of(PERSON_ID));
	}

	/**
	 * FamilyCare answers with personal identity numbers — the interface default. LifecareCaseService branches on this
	 * to decide whether a person in a response still has to be resolved to a partyId, so a silent flip here would put
	 * personnummer into partyId fields.
	 */
	@Test
	void respondsWithPersonalIdentityNumbersNotPartyIds() {
		assertThat(integration.respondsWithPartyId()).isFalse();
		verifyNoInteractions(clientMock);
	}

	/**
	 * A party id the citizen service does not know never reaches FamilyCare: the read fails as the citizen lookup it
	 * is, not as a Lifecare error.
	 */
	@Test
	void unknownPartyIdFailsBeforeFamilyCareIsCalled() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, CHILD_PARTY_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> integration.getCalculations(MUNICIPALITY_ID, CHILD_PARTY_ID, START, END))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.extracting(throwable -> ((ThrowableProblem) throwable).getDetail())
			.isEqualTo("No citizen found for partyId " + CHILD_PARTY_ID);

		verifyNoInteractions(clientMock);
	}

	// ---- Person-based reads ------------------------------------------------------------------------------------------

	@Test
	void getPerson() {
		final var response = new PersonBasedPersonDTO();
		when(clientMock.getPerson(PERSON_ID)).thenReturn(response);

		assertThat(integration.getPerson(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).isSameAs(response);
		verify(clientMock).getPerson(PERSON_ID);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getContacts() {
		final var response = List.of(new PersonBasedContactDTO());
		when(clientMock.getContacts(PERSON_ID)).thenReturn(response);

		assertThat(integration.getContacts(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).isSameAs(response);
		verify(clientMock).getContacts(PERSON_ID);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getActualisations() {
		final var response = new ApiPaginationCompositePersonBasedAktualiseringDTO();
		when(clientMock.getActualisations(PERSON_ID, START_WIRE, END_WIRE, null, null, false)).thenReturn(response);

		assertThat(integration.getActualisations(MUNICIPALITY_ID, APPLICANT_PARTY_ID, START, END)).isSameAs(response);
		verify(clientMock).getActualisations(PERSON_ID, START_WIRE, END_WIRE, null, null, false);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getCalculations() {
		final var response = new ApiPaginationCompositePersonBasedCalculationDTO();
		when(clientMock.getCalculations(PERSON_ID, START_WIRE, END_WIRE, null, null, false)).thenReturn(response);

		assertThat(integration.getCalculations(MUNICIPALITY_ID, APPLICANT_PARTY_ID, START, END)).isSameAs(response);
		verify(clientMock).getCalculations(PERSON_ID, START_WIRE, END_WIRE, null, null, false);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getDecisions() {
		final var response = new ApiPaginationCompositePersonBasedDecisionDTO();
		when(clientMock.getDecisions(PERSON_ID, START_WIRE, END_WIRE, null, null, false)).thenReturn(response);

		assertThat(integration.getDecisions(MUNICIPALITY_ID, APPLICANT_PARTY_ID, START, END)).isSameAs(response);
		verify(clientMock).getDecisions(PERSON_ID, START_WIRE, END_WIRE, null, null, false);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getPayments() {
		final var response = new ApiPaginationCompositePersonBasedPaymentDTO();
		when(clientMock.getPayments(PERSON_ID, START_WIRE, END_WIRE, null, null, false)).thenReturn(response);

		assertThat(integration.getPayments(MUNICIPALITY_ID, APPLICANT_PARTY_ID, START, END)).isSameAs(response);
		verify(clientMock).getPayments(PERSON_ID, START_WIRE, END_WIRE, null, null, false);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getInvestigations() {
		final var response = new ApiPaginationCompositePersonBasedInvestigationDTO();
		when(clientMock.getInvestigations(PERSON_ID, START_WIRE, END_WIRE, null, null, false)).thenReturn(response);

		assertThat(integration.getInvestigations(MUNICIPALITY_ID, APPLICANT_PARTY_ID, START, END)).isSameAs(response);
		verify(clientMock).getInvestigations(PERSON_ID, START_WIRE, END_WIRE, null, null, false);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getServices() {
		final var response = new ApiPaginationCompositePersonBasedServiceDTO();
		when(clientMock.getServices(PERSON_ID, START_WIRE, END_WIRE, null, null, false)).thenReturn(response);

		assertThat(integration.getServices(MUNICIPALITY_ID, APPLICANT_PARTY_ID, START, END)).isSameAs(response);
		verify(clientMock).getServices(PERSON_ID, START_WIRE, END_WIRE, null, null, false);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getExecutions() {
		final var response = new ApiPaginationCompositePersonBasedExecutionDTO();
		when(clientMock.getExecutions(PERSON_ID, START_WIRE, END_WIRE, null, null, false)).thenReturn(response);

		assertThat(integration.getExecutions(MUNICIPALITY_ID, APPLICANT_PARTY_ID, START, END)).isSameAs(response);
		verify(clientMock).getExecutions(PERSON_ID, START_WIRE, END_WIRE, null, null, false);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getUsers() {
		final var response = List.of(new User().id("9001").fullName("Anna Andersson").networkUserId("anna01ker"));
		when(clientMock.getUsers(1000, null, null, null)).thenReturn(response);

		assertThat(integration.getUsers(MUNICIPALITY_ID, 1000, null, null, null)).isSameAs(response);
		verify(clientMock).getUsers(1000, null, null, null);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getUsersFailureBecomesBadGateway() {
		when(clientMock.getUsers(1000, null, null, null)).thenThrow(Problem.valueOf(NOT_FOUND, "boom"));

		assertThatThrownBy(() -> integration.getUsers(MUNICIPALITY_ID, 1000, null, null, null))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.extracting(throwable -> ((ThrowableProblem) throwable).getDetail())
			.isEqualTo("Error fetching users in Lifecare FamilyCare: 404 Not Found: boom");
	}

	@Test
	void getResourceAllocations() {
		final var response = new ApiPaginationCompositePersonBasedResourceAllocationDTO();
		when(clientMock.getResourceAllocations(PERSON_ID, START_WIRE, END_WIRE, null, null, false)).thenReturn(response);

		assertThat(integration.getResourceAllocations(MUNICIPALITY_ID, APPLICANT_PARTY_ID, START, END)).isSameAs(response);
		verify(clientMock).getResourceAllocations(PERSON_ID, START_WIRE, END_WIRE, null, null, false);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getDecisionsFailure() {
		when(clientMock.getDecisions(PERSON_ID, START_WIRE, END_WIRE, null, null, false)).thenThrow(new RuntimeException("timeout"));

		assertThatThrownBy(() -> integration.getDecisions(MUNICIPALITY_ID, APPLICANT_PARTY_ID, START, END))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.extracting(throwable -> ((ThrowableProblem) throwable).getDetail())
			.isEqualTo("Error fetching decision in Lifecare FamilyCare: RuntimeException");

		verify(clientMock).getDecisions(PERSON_ID, START_WIRE, END_WIRE, null, null, false);
	}

	// ---- Write-back + proposals --------------------------------------------------------------------------------------

	@Test
	void getActualisationProposal() {
		final var response = new PersonBasedAktualiseringProposalDTO();
		when(clientMock.getActualisationProposal(PERSON_ID)).thenReturn(response);

		assertThat(integration.getActualisationProposal(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).isSameAs(response);
		verify(clientMock).getActualisationProposal(PERSON_ID);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getActualisationProposalFailure() {
		when(clientMock.getActualisationProposal(PERSON_ID)).thenThrow(Problem.valueOf(NOT_FOUND, "boom"));

		assertThatThrownBy(() -> integration.getActualisationProposal(MUNICIPALITY_ID, APPLICANT_PARTY_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.extracting(throwable -> ((ThrowableProblem) throwable).getDetail())
			.isEqualTo("Error fetching actualisation proposal in Lifecare FamilyCare: 404 Not Found: boom");

		verify(clientMock).getActualisationProposal(PERSON_ID);
	}

	@Test
	void createActualisation() {
		final var body = new PostAktualiseringsBodyRequest().personId(APPLICANT_PARTY_ID);
		when(clientMock.createActualisation(body)).thenReturn(4711);

		assertThat(integration.createActualisation(MUNICIPALITY_ID, body)).isEqualTo(4711);
		assertThat(body.getPersonId()).isEqualTo(PERSON_ID);
		verify(clientMock).createActualisation(body);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void createActualisationFailure() {
		final var body = new PostAktualiseringsBodyRequest().personId(APPLICANT_PARTY_ID);
		when(clientMock.createActualisation(body)).thenThrow(new RuntimeException("connection reset"));

		assertThatThrownBy(() -> integration.createActualisation(MUNICIPALITY_ID, body))
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

		assertThat(integration.getCalculationProposal(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).isSameAs(response);
		verify(clientMock).getCalculationProposal(PERSON_ID);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void createCalculation() {
		final var body = new PostCalculationBodyRequest().personId(APPLICANT_PARTY_ID);
		when(clientMock.createCalculation(body)).thenReturn(99);

		assertThat(integration.createCalculation(MUNICIPALITY_ID, body)).isEqualTo(99);
		assertThat(body.getPersonId()).isEqualTo(PERSON_ID);
		verify(clientMock).createCalculation(body);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void createCalculationFailure() {
		final var body = new PostCalculationBodyRequest().personId(APPLICANT_PARTY_ID);
		when(clientMock.createCalculation(body)).thenThrow(Problem.valueOf(BAD_GATEWAY, "upstream down"));

		assertThatThrownBy(() -> integration.createCalculation(MUNICIPALITY_ID, body))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.extracting(throwable -> ((ThrowableProblem) throwable).getDetail())
			.isEqualTo("Error creating calculation in Lifecare FamilyCare: 502 Bad Gateway: upstream down");

		verify(clientMock).createCalculation(body);
	}

	/**
	 * FamilyCare keys every {@code PersonId} on the personal identity number, including the ones on
	 * {@code CalculationPersons} (confirmed with Tieto 2026-09-22). careM assembles those rows from party ids, so this
	 * route resolves them before sending — otherwise the calculation would reach FamilyCare naming its household in an
	 * identity space FamilyCare has never heard of.
	 */
	@Test
	void createCalculationResolvesHouseholdPartyIdsToPersonalIdentityNumbers() {
		final var body = new PostCalculationBodyRequest()
			.personId(APPLICANT_PARTY_ID)
			.calculationPersons(List.of(
				new PersonBasedCalculationPersonPostDTO().personId(APPLICANT_PARTY_ID).numberOfDays(30),
				new PersonBasedCalculationPersonPostDTO().personId(CHILD_PARTY_ID).numberOfDays(15)));

		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of(PERSON_ID));
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, CHILD_PARTY_ID)).thenReturn(Optional.of(CHILD_PERSON_ID));
		when(clientMock.createCalculation(body)).thenReturn(99);

		assertThat(integration.createCalculation(MUNICIPALITY_ID, body)).isEqualTo(99);

		final var sent = ArgumentCaptor.forClass(PostCalculationBodyRequest.class);
		verify(clientMock).createCalculation(sent.capture());
		assertThat(sent.getValue().getPersonId()).isEqualTo(PERSON_ID);
		assertThat(sent.getValue().getCalculationPersons())
			.extracting(PersonBasedCalculationPersonPostDTO::getPersonId, PersonBasedCalculationPersonPostDTO::getNumberOfDays)
			.containsExactly(tuple(PERSON_ID, 30), tuple(CHILD_PERSON_ID, 15));
		verifyNoMoreInteractions(clientMock);
	}

	/**
	 * A household member that cannot be resolved fails the whole calculation. Sending the calculation without the row
	 * would shrink the household the norm is computed from, silently and with nothing on the errand to explain it.
	 */
	@Test
	void createCalculationFailsWhenAHouseholdMemberHasNoPersonalIdentityNumber() {
		final var body = new PostCalculationBodyRequest()
			.personId(APPLICANT_PARTY_ID)
			.calculationPersons(List.of(
				new PersonBasedCalculationPersonPostDTO().personId(APPLICANT_PARTY_ID),
				new PersonBasedCalculationPersonPostDTO().personId(CHILD_PARTY_ID)));

		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of(PERSON_ID));
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, CHILD_PARTY_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> integration.createCalculation(MUNICIPALITY_ID, body))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.extracting(throwable -> ((ThrowableProblem) throwable).getDetail())
			.isEqualTo("No personal identity number could be resolved for a person on the calculation");

		verifyNoInteractions(clientMock);
	}

	/** A row with no party id at all is the same failure, without spending a citizen lookup on the row. */
	@Test
	void createCalculationFailsWhenAHouseholdMemberHasNoPartyId() {
		final var body = new PostCalculationBodyRequest()
			.personId(APPLICANT_PARTY_ID)
			.calculationPersons(List.of(new PersonBasedCalculationPersonPostDTO().personId(" ")));

		assertThatThrownBy(() -> integration.createCalculation(MUNICIPALITY_ID, body))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY);

		verify(citizenServiceMock).getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID);
		verifyNoMoreInteractions(citizenServiceMock);
		verifyNoInteractions(clientMock);
	}

	@Test
	void postActualisationAttachmentWrapsBytesAsTheContentPart() throws Exception {
		final var content = new byte[] {
			9, 8, 7
		};

		integration.postActualisationAttachment(MUNICIPALITY_ID, 4711, "DOC", "SENDER", "Title", "Sender", "EB-1_meddelandehistorik.pdf", content);

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

		assertThatThrownBy(() -> integration.postActualisationAttachment(MUNICIPALITY_ID, 4711, "DOC", "SENDER", "Title", "Sender", "f.pdf", new byte[] {
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

		assertThatThrownBy(() -> integration.getPerson(MUNICIPALITY_ID, APPLICANT_PARTY_ID))
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

		assertThatThrownBy(() -> integration.getPerson(MUNICIPALITY_ID, APPLICANT_PARTY_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.extracting(throwable -> ((ThrowableProblem) throwable).getDetail())
			.satisfies(detail -> assertThat(detail).contains("404").contains("person not found"));
	}
}
