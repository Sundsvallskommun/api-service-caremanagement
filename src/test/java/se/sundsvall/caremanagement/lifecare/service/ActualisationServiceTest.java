package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringProposalDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsInfoDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsInvestigationDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsInvestigationTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsServiceDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsServiceTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PostAktualiseringsBodyRequest;
import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFamilyCareIntegration;
import se.sundsvall.dept44.problem.Problem;
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
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.CONFLICT;

@ExtendWith(MockitoExtension.class)
class ActualisationServiceTest {

	private static final String MUNICIPALITY_ID = "2281";

	private static final String APPLICANT = "199001011234";
	private static final LocalDate DATE = LocalDate.of(2026, JUNE, 1);
	private static final String RE_APPLICATION_TYPE = "Ek Återansökan Digital Ekonomiskt bistånd";
	private static final Predicate<Integer> CLAIMED_NOWHERE = id -> false;

	@Mock
	private LifecareFamilyCareIntegration lifecareFamilyCareIntegrationMock;

	@Mock
	private CaseworkerResolver caseworkerResolverMock;

	/** The real record, not a mock: it is configuration, and the assembler reads every field off it. */
	private final ActualisationProperties names = new ActualisationProperties(
		"Ek Återansökan Digital Ekonomiskt bistånd", "Den enskilde", "Ekonomiskt bistånd", "Ekonomiskt bistånd",
		"EK Nyansökan Digital Ekonomiskt bistånd");

	private ActualisationService service;

	@BeforeEach
	void setUp() {
		service = new ActualisationService(lifecareFamilyCareIntegrationMock, caseworkerResolverMock, names);
	}

	@Test
	void createReturnsTheInsatsTheActualisationWasLinkedTo() {
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO().id(3).name("Ek Återansökan Digital Ekonomiskt bistånd")
				.addServiceTypesItem(new PersonBasedAktualiseringsServiceTypeDTO().id(27)))
			.addServicesItem(new PersonBasedAktualiseringsServiceDTO().id(7700).type(27));
		when(caseworkerResolverMock.resolve(MUNICIPALITY_ID, APPLICANT, DATE)).thenReturn(Optional.empty());
		when(lifecareFamilyCareIntegrationMock.getActualisationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal);
		when(lifecareFamilyCareIntegrationMock.createActualisation(eq(MUNICIPALITY_ID), any(PostAktualiseringsBodyRequest.class))).thenReturn(5012);

		final var result = service.createActualisation(MUNICIPALITY_ID, APPLICANT, DATE, false);

		assertThat(result.serviceId()).isEqualTo(7700);
	}

	@Test
	void createForNewApplicationUsesTheNyansokanTypeAndLinksNoInsats() {
		// The shape of Lifecare's catalogue: the återansökan type links the EB insats, the nyansökan type an
		// ekonomiutredning. A nyansökan filed under the återansökan type is refused with 403 "Aktualisering".
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO().id(28).name("EK Nyansökan Digital Ekonomiskt bistånd")
				.addInvestigationTypesItem(new PersonBasedAktualiseringsInvestigationTypeDTO().id(21)))
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO().id(29).name("EK Återansökan Digital Ekonomiskt bistånd")
				.addServiceTypesItem(new PersonBasedAktualiseringsServiceTypeDTO().id(27)))
			.addServicesItem(new PersonBasedAktualiseringsServiceDTO().id(7700).type(27))
			// An ekonomiutredning the proposal offers without saying it is closed - a nyansökan must not take it.
			.addInvestigationsItem(new PersonBasedAktualiseringsInvestigationDTO().id(54).type(21));
		when(caseworkerResolverMock.resolve(MUNICIPALITY_ID, APPLICANT, DATE)).thenReturn(Optional.empty());
		when(lifecareFamilyCareIntegrationMock.getActualisationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal);
		when(lifecareFamilyCareIntegrationMock.createActualisation(eq(MUNICIPALITY_ID), any(PostAktualiseringsBodyRequest.class))).thenReturn(5012);

		final var result = service.createActualisation(MUNICIPALITY_ID, APPLICANT, DATE, true);

		final var bodyCaptor = ArgumentCaptor.forClass(PostAktualiseringsBodyRequest.class);
		verify(lifecareFamilyCareIntegrationMock).createActualisation(eq(MUNICIPALITY_ID), bodyCaptor.capture());
		assertThat(bodyCaptor.getValue().getType()).isEqualTo(28);
		assertThat(bodyCaptor.getValue().getServiceId()).isNull();
		assertThat(bodyCaptor.getValue().getInvestigationId()).isNull();
		assertThat(result.serviceId()).isNull();
	}

	@Test
	void findFinancialAssistanceServiceIdPicksTheServiceTheActualisationTypeAccepts() {
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO().id(3).name("Ek Återansökan Digital Ekonomiskt bistånd")
				.addServiceTypesItem(new PersonBasedAktualiseringsServiceTypeDTO().id(27)))
			.addServicesItem(new PersonBasedAktualiseringsServiceDTO().id(5).type(3))
			.addServicesItem(new PersonBasedAktualiseringsServiceDTO().id(7700).type(27));
		when(lifecareFamilyCareIntegrationMock.getActualisationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal);

		assertThat(service.findFinancialAssistanceServiceId(MUNICIPALITY_ID, APPLICANT)).contains(7700);
	}

	@Test
	void findFinancialAssistanceServiceIdIsEmptyWhenThePersonHasNoOpenEbInsats() {
		when(lifecareFamilyCareIntegrationMock.getActualisationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(new PersonBasedAktualiseringProposalDTO());

		assertThat(service.findFinancialAssistanceServiceId(MUNICIPALITY_ID, APPLICANT)).isEmpty();
	}

	@Test
	void createResolvesCaseworkerAssemblesAndPosts() {
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO().id(3));

		when(caseworkerResolverMock.resolve(MUNICIPALITY_ID, APPLICANT, DATE)).thenReturn(Optional.of(new ResolvedCaseworker("9001", "anna01ker", "Anna Andersson")));
		when(lifecareFamilyCareIntegrationMock.getActualisationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal);
		when(lifecareFamilyCareIntegrationMock.createActualisation(eq(MUNICIPALITY_ID), any(PostAktualiseringsBodyRequest.class))).thenReturn(5012);

		final var result = service.createActualisation(MUNICIPALITY_ID, APPLICANT, DATE, false);

		assertThat(result.actualisationId()).isEqualTo(5012);
		assertThat(result.assignedUserId()).isEqualTo("anna01ker");

		final ArgumentCaptor<PostAktualiseringsBodyRequest> captor = ArgumentCaptor.forClass(PostAktualiseringsBodyRequest.class);
		verify(lifecareFamilyCareIntegrationMock).createActualisation(eq(MUNICIPALITY_ID), captor.capture());
		assertThat(captor.getValue().getPersonId()).isEqualTo(APPLICANT);
		assertThat(captor.getValue().getDate()).isEqualTo("2026-06-01T00:00:00");
		assertThat(captor.getValue().getType()).isEqualTo(3);
		assertThat(captor.getValue().getCaseworkerId()).isEqualTo("9001");
	}

	@Test
	void createWithoutResolvableCaseworkerStillPostsWithoutCaseworkerOrAssignee() {
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO().id(3));

		when(caseworkerResolverMock.resolve(MUNICIPALITY_ID, APPLICANT, DATE)).thenReturn(Optional.empty());
		when(lifecareFamilyCareIntegrationMock.getActualisationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal);
		when(lifecareFamilyCareIntegrationMock.createActualisation(eq(MUNICIPALITY_ID), any(PostAktualiseringsBodyRequest.class))).thenReturn(5012);

		final var result = service.createActualisation(MUNICIPALITY_ID, APPLICANT, DATE, false);

		assertThat(result.actualisationId()).isEqualTo(5012);
		assertThat(result.assignedUserId()).isNull();

		final ArgumentCaptor<PostAktualiseringsBodyRequest> captor = ArgumentCaptor.forClass(PostAktualiseringsBodyRequest.class);
		verify(lifecareFamilyCareIntegrationMock).createActualisation(eq(MUNICIPALITY_ID), captor.capture());
		assertThat(captor.getValue().getCaseworkerId()).isNull();
	}

	@Test
	void createTreatsCaseworkerResolutionFailureAsBestEffort() {
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO().id(3));

		when(caseworkerResolverMock.resolve(MUNICIPALITY_ID, APPLICANT, DATE)).thenThrow(new RuntimeException("FamilyCare down"));
		when(lifecareFamilyCareIntegrationMock.getActualisationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal);
		when(lifecareFamilyCareIntegrationMock.createActualisation(eq(MUNICIPALITY_ID), any(PostAktualiseringsBodyRequest.class))).thenReturn(5012);

		final var result = service.createActualisation(MUNICIPALITY_ID, APPLICANT, DATE, false);

		assertThat(result.actualisationId()).isEqualTo(5012);
		assertThat(result.assignedUserId()).isNull();

		final ArgumentCaptor<PostAktualiseringsBodyRequest> captor = ArgumentCaptor.forClass(PostAktualiseringsBodyRequest.class);
		verify(lifecareFamilyCareIntegrationMock).createActualisation(eq(MUNICIPALITY_ID), captor.capture());
		assertThat(captor.getValue().getCaseworkerId()).isNull();
	}

	@Test
	void listFormatsDatesMapsResultAndDropsPersonId() {
		final var dto = new PersonBasedAktualiseringDTO()
			.id(5012).type("Ansökan").personId(APPLICANT).name("Ekonomiskt bistånd").date("2026-06-01")
			.reason("Nyansökan").regards("Försörjningsstöd").fromWho("Den enskilde").caseworker("Anna Andersson")
			.organization("IFO").status("Pågående").investigationId(8801).serviceId(7700).decisionId(9900);
		when(lifecareFamilyCareIntegrationMock.getActualisations(MUNICIPALITY_ID, APPLICANT, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-06-30")))
			.thenReturn(new ApiPaginationCompositePersonBasedAktualiseringDTO().addResultItem(dto));

		final var result = service.listActualisations(MUNICIPALITY_ID, APPLICANT, LocalDate.of(2026, JANUARY, 1), LocalDate.of(2026, JUNE, 30));

		assertThat(result).singleElement().satisfies(summary -> {
			assertThat(summary.id()).isEqualTo(5012);
			assertThat(summary.type()).isEqualTo("Ansökan");
			assertThat(summary.name()).isEqualTo("Ekonomiskt bistånd");
			assertThat(summary.date()).isEqualTo("2026-06-01");
			assertThat(summary.reason()).isEqualTo("Nyansökan");
			assertThat(summary.regards()).isEqualTo("Försörjningsstöd");
			assertThat(summary.fromWho()).isEqualTo("Den enskilde");
			assertThat(summary.caseworker()).isEqualTo("Anna Andersson");
			assertThat(summary.organization()).isEqualTo("IFO");
			assertThat(summary.status()).isEqualTo("Pågående");
			assertThat(summary.investigationId()).isEqualTo(8801);
			assertThat(summary.serviceId()).isEqualTo(7700);
			assertThat(summary.decisionId()).isEqualTo(9900);
		});
	}

	@Test
	void listReturnsEmptyWhenFamilyCareHasNoPage() {
		when(lifecareFamilyCareIntegrationMock.getActualisations(MUNICIPALITY_ID, APPLICANT, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-06-30"))).thenReturn(null);

		assertThat(service.listActualisations(MUNICIPALITY_ID, APPLICANT, LocalDate.of(2026, JANUARY, 1), LocalDate.of(2026, JUNE, 30))).isEmpty();
	}

	@Test
	void uploadAttachmentSendsPdfToTheActualisation() {
		final var content = new byte[] {
			1, 2, 3
		};

		service.uploadAttachment(MUNICIPALITY_ID, 5012,
			new AttachmentUpload("MEDDELANDEHISTORIK", "MYNDIGHET", "Meddelandehistorik", "Sundsvalls kommun", "EB-26060001_meddelandehistorik.pdf", content));

		verify(lifecareFamilyCareIntegrationMock).postActualisationAttachment(MUNICIPALITY_ID, 5012,
			new AttachmentUpload("MEDDELANDEHISTORIK", "MYNDIGHET", "Meddelandehistorik", "Sundsvalls kommun", "EB-26060001_meddelandehistorik.pdf", content));
	}

	// ---- createOrAdoptActualisation: a retry must never make a second actualisation --------------------------------

	private static PersonBasedAktualiseringProposalDTO proposalWithTheReApplicationType() {
		return new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO().id(3).name(RE_APPLICATION_TYPE)
				.addServiceTypesItem(new PersonBasedAktualiseringsServiceTypeDTO().id(27)))
			.addServicesItem(new PersonBasedAktualiseringsServiceDTO().id(7700).type(27));
	}

	private static PersonBasedAktualiseringDTO listed(final int id, final String type, final String date) {
		return new PersonBasedAktualiseringDTO().id(id).type(type).personId(APPLICANT).date(date).serviceId(7700);
	}

	private void proposalAndCaseworkerAreAvailable() {
		when(caseworkerResolverMock.resolve(MUNICIPALITY_ID, APPLICANT, DATE)).thenReturn(Optional.of(new ResolvedCaseworker("9001", "anna01ker", "Anna Andersson")));
		when(lifecareFamilyCareIntegrationMock.getActualisationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposalWithTheReApplicationType());
	}

	private void lifecareListsOnTheIntakeDate(final PersonBasedAktualiseringDTO... actualisations) {
		final var page = new ApiPaginationCompositePersonBasedAktualiseringDTO();
		for (final var actualisation : actualisations) {
			page.addResultItem(actualisation);
		}
		when(lifecareFamilyCareIntegrationMock.getActualisations(MUNICIPALITY_ID, APPLICANT, DATE, DATE)).thenReturn(page);
	}

	@Test
	void anEarlierAttemptsActualisationIsAdoptedInsteadOfCreatingASecond() {
		proposalAndCaseworkerAreAvailable();
		lifecareListsOnTheIntakeDate(listed(5012, RE_APPLICATION_TYPE, "2026-06-01"));

		final var result = service.createOrAdoptActualisation(MUNICIPALITY_ID, APPLICANT, DATE, false, CLAIMED_NOWHERE);

		assertThat(result.adopted()).isTrue();
		assertThat(result.actualisationId()).isEqualTo(5012);
		// What Lifecare holds on the actualisation, and the caseworker the create would have set.
		assertThat(result.serviceId()).isEqualTo(7700);
		assertThat(result.assignedUserId()).isEqualTo("anna01ker");
		verify(lifecareFamilyCareIntegrationMock, never()).createActualisation(any(), any());
	}

	@Test
	void whenNoEarlierActualisationExistsItIsCreated() {
		proposalAndCaseworkerAreAvailable();
		lifecareListsOnTheIntakeDate();
		when(lifecareFamilyCareIntegrationMock.createActualisation(eq(MUNICIPALITY_ID), any(PostAktualiseringsBodyRequest.class))).thenReturn(5013);

		final var result = service.createOrAdoptActualisation(MUNICIPALITY_ID, APPLICANT, DATE, false, CLAIMED_NOWHERE);

		assertThat(result.adopted()).isFalse();
		assertThat(result.actualisationId()).isEqualTo(5013);
		assertThat(result.assignedUserId()).isEqualTo("anna01ker");
		assertThat(result.serviceId()).isEqualTo(7700);
	}

	@Test
	void twoActualisationsThatCouldBeTheEarlierAttemptsFailWithoutCreatingAnything() {
		proposalAndCaseworkerAreAvailable();
		lifecareListsOnTheIntakeDate(listed(5012, RE_APPLICATION_TYPE, "2026-06-01"), listed(5013, RE_APPLICATION_TYPE, "2026-06-01"));

		assertThatThrownBy(() -> service.createOrAdoptActualisation(MUNICIPALITY_ID, APPLICANT, DATE, false, CLAIMED_NOWHERE))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", CONFLICT)
			// The ids are Lifecare's, the message carries nothing about the person.
			.hasMessageContaining("2 actualisations")
			.hasMessageContaining("5012, 5013")
			.satisfies(problem -> assertThat(problem.getMessage()).doesNotContain(APPLICANT));

		verify(lifecareFamilyCareIntegrationMock, never()).createActualisation(any(), any());
	}

	@Test
	void aLookupThatFailsFailsTheCallWithoutCreatingAnything() {
		proposalAndCaseworkerAreAvailable();
		when(lifecareFamilyCareIntegrationMock.getActualisations(MUNICIPALITY_ID, APPLICANT, DATE, DATE))
			.thenThrow(Problem.valueOf(BAD_GATEWAY, "Error fetching actualisations in Lifecare FamilyCare: 503"));

		assertThatThrownBy(() -> service.createOrAdoptActualisation(MUNICIPALITY_ID, APPLICANT, DATE, false, CLAIMED_NOWHERE))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY);

		verify(lifecareFamilyCareIntegrationMock, never()).createActualisation(any(), any());
	}

	@Test
	void anActualisationAnotherErrandAlreadyHoldsIsNotTheEarlierAttempts() {
		proposalAndCaseworkerAreAvailable();
		// 5012 belongs to another errand of the same person on the same day; 5013 is the one this errand's attempt made.
		lifecareListsOnTheIntakeDate(listed(5012, RE_APPLICATION_TYPE, "2026-06-01"), listed(5013, RE_APPLICATION_TYPE, "2026-06-01"));

		final var result = service.createOrAdoptActualisation(MUNICIPALITY_ID, APPLICANT, DATE, false, id -> id == 5012);

		assertThat(result.adopted()).isTrue();
		assertThat(result.actualisationId()).isEqualTo(5013);
		verify(lifecareFamilyCareIntegrationMock, never()).createActualisation(any(), any());
	}

	@Test
	void anActualisationAnotherErrandHoldsIsNeverAdoptedSoTheAttemptCreatesItsOwn() {
		proposalAndCaseworkerAreAvailable();
		lifecareListsOnTheIntakeDate(listed(5012, RE_APPLICATION_TYPE, "2026-06-01"));
		when(lifecareFamilyCareIntegrationMock.createActualisation(eq(MUNICIPALITY_ID), any(PostAktualiseringsBodyRequest.class))).thenReturn(5013);

		final var result = service.createOrAdoptActualisation(MUNICIPALITY_ID, APPLICANT, DATE, false, id -> id == 5012);

		assertThat(result.adopted()).isFalse();
		assertThat(result.actualisationId()).isEqualTo(5013);
	}

	@Test
	void anotherTypeOrAnotherDayIsNotTheEarlierAttempt() {
		proposalAndCaseworkerAreAvailable();
		lifecareListsOnTheIntakeDate(
			listed(5010, "Vux Utredning 14 kap 2 § SoL", "2026-06-01"),
			listed(5011, RE_APPLICATION_TYPE, "2026-06-02"),
			listed(5012, RE_APPLICATION_TYPE, null),
			new PersonBasedAktualiseringDTO().id(null).type(RE_APPLICATION_TYPE).date("2026-06-01"));
		when(lifecareFamilyCareIntegrationMock.createActualisation(eq(MUNICIPALITY_ID), any(PostAktualiseringsBodyRequest.class))).thenReturn(5013);

		final var result = service.createOrAdoptActualisation(MUNICIPALITY_ID, APPLICANT, DATE, false, CLAIMED_NOWHERE);

		assertThat(result.adopted()).isFalse();
		assertThat(result.actualisationId()).isEqualTo(5013);
	}

	@Test
	void theTypeAndTheDateAreMatchedTheWayFamilyCareWritesThem() {
		proposalAndCaseworkerAreAvailable();
		// Case and whitespace differ from the catalogue, and the date carries a time of day.
		lifecareListsOnTheIntakeDate(listed(5012, "  ek återansökan digital ekonomiskt bistånd ", "2026-06-01T00:00:00"));

		final var result = service.createOrAdoptActualisation(MUNICIPALITY_ID, APPLICANT, DATE, false, CLAIMED_NOWHERE);

		assertThat(result.adopted()).isTrue();
		assertThat(result.actualisationId()).isEqualTo(5012);
	}

	@Test
	void aTypeThatFellBackToTheFirstOfferedIsMatchedByTheNameItWasGiven() {
		// The configured name is not in the catalogue, so the create used the first offered type. The earlier attempt's
		// actualisation carries that one's name, not the configured one.
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO().id(9).name("Renamed återansökan type"));
		when(caseworkerResolverMock.resolve(MUNICIPALITY_ID, APPLICANT, DATE)).thenReturn(Optional.empty());
		when(lifecareFamilyCareIntegrationMock.getActualisationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal);
		lifecareListsOnTheIntakeDate(listed(5012, "Renamed återansökan type", "2026-06-01"));

		final var result = service.createOrAdoptActualisation(MUNICIPALITY_ID, APPLICANT, DATE, false, CLAIMED_NOWHERE);

		assertThat(result.adopted()).isTrue();
		assertThat(result.actualisationId()).isEqualTo(5012);
		assertThat(result.assignedUserId()).isNull();
	}

	@Test
	void aNyansokanLooksForTheNyansokanType() {
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO().id(28).name("EK Nyansökan Digital Ekonomiskt bistånd"))
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO().id(29).name(RE_APPLICATION_TYPE));
		when(caseworkerResolverMock.resolve(MUNICIPALITY_ID, APPLICANT, DATE)).thenReturn(Optional.empty());
		when(lifecareFamilyCareIntegrationMock.getActualisationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal);
		lifecareListsOnTheIntakeDate(listed(5011, RE_APPLICATION_TYPE, "2026-06-01"), listed(5012, "EK Nyansökan Digital Ekonomiskt bistånd", "2026-06-01"));

		final var result = service.createOrAdoptActualisation(MUNICIPALITY_ID, APPLICANT, DATE, true, CLAIMED_NOWHERE);

		assertThat(result.adopted()).isTrue();
		assertThat(result.actualisationId()).isEqualTo(5012);
	}

	@Test
	void theSameActualisationListedTwiceIsOneCandidate() {
		proposalAndCaseworkerAreAvailable();
		lifecareListsOnTheIntakeDate(listed(5012, RE_APPLICATION_TYPE, "2026-06-01"), listed(5012, RE_APPLICATION_TYPE, "2026-06-01"));

		final var result = service.createOrAdoptActualisation(MUNICIPALITY_ID, APPLICANT, DATE, false, CLAIMED_NOWHERE);

		assertThat(result.adopted()).isTrue();
		assertThat(result.actualisationId()).isEqualTo(5012);
	}

	@Test
	void aProposalWithoutTheTypeLooksForNothingAndLeavesTheCreateToFailOnIt() {
		when(caseworkerResolverMock.resolve(MUNICIPALITY_ID, APPLICANT, DATE)).thenReturn(Optional.empty());
		when(lifecareFamilyCareIntegrationMock.getActualisationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(new PersonBasedAktualiseringProposalDTO());
		when(lifecareFamilyCareIntegrationMock.createActualisation(eq(MUNICIPALITY_ID), any(PostAktualiseringsBodyRequest.class)))
			.thenThrow(Problem.valueOf(BAD_GATEWAY, "Error creating actualisation in Lifecare FamilyCare: 400"));

		assertThatThrownBy(() -> service.createOrAdoptActualisation(MUNICIPALITY_ID, APPLICANT, DATE, false, CLAIMED_NOWHERE))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY);

		verify(lifecareFamilyCareIntegrationMock, never()).getActualisations(any(), any(), any(), any());
	}

	@Test
	void createActualisationNeverLooksForAnEarlierAttempt() {
		proposalAndCaseworkerAreAvailable();
		when(lifecareFamilyCareIntegrationMock.createActualisation(eq(MUNICIPALITY_ID), any(PostAktualiseringsBodyRequest.class))).thenReturn(5012);

		final var result = service.createActualisation(MUNICIPALITY_ID, APPLICANT, DATE, false);

		assertThat(result.adopted()).isFalse();
		verify(lifecareFamilyCareIntegrationMock, never()).getActualisations(any(), any(), any(), any());
	}
}
