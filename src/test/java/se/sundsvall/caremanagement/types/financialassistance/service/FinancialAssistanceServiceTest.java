package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.attachments.service.AttachmentService;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.api.model.PatchErrand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.decisions.api.model.Decision;
import se.sundsvall.caremanagement.decisions.service.DecisionService;
import se.sundsvall.caremanagement.lifecare.service.ActualisationService;
import se.sundsvall.caremanagement.lifecare.service.NormberakningService;
import se.sundsvall.caremanagement.lifecare.service.PaymentStatus;
import se.sundsvall.caremanagement.lifecare.service.PaymentStatusService;
import se.sundsvall.caremanagement.lifecare.service.model.Completeness;
import se.sundsvall.caremanagement.lifecare.service.model.EffectiveIncome;
import se.sundsvall.caremanagement.lifecare.service.model.NormberakningHeader;
import se.sundsvall.caremanagement.stakeholders.api.model.ContactChannel;
import se.sundsvall.caremanagement.stakeholders.api.model.Stakeholder;
import se.sundsvall.caremanagement.stakeholders.service.StakeholderService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ActualisationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateFinancialAssistanceRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceData;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormHeaderInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormberakningDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormberakningRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Person;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApproval;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApprovals;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Warning;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormExpenseEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormIncomeEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormPersonEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormberakningDraftEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_NEW;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_RENEWAL;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_INKOMMEN;

@ExtendWith(MockitoExtension.class)
class FinancialAssistanceServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private FinancialAssistanceRepository repositoryMock;

	@Mock
	private NormberakningService normberakningServiceMock;

	@Mock
	private ActualisationService actualisationServiceMock;

	@Mock
	private PaymentStatusService paymentStatusServiceMock;

	@Mock
	private CitizenService citizenServiceMock;

	@Mock
	private DecisionService decisionServiceMock;

	@Mock
	private AttachmentService attachmentServiceMock;

	@Mock
	private StakeholderService stakeholderServiceMock;

	@Mock
	private WarningService warningServiceMock;

	@Mock
	private SectionApprovalService sectionApprovalServiceMock;

	@Mock
	private DraftService draftServiceMock;

	@Mock
	private NormberakningFeeder normberakningFeederMock;

	@InjectMocks
	private FinancialAssistanceService service;

	private static final String APPLICANT_PARTY_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
	private static final String CO_APPLICANT_PARTY_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";

	@Test
	void createBuildsEnvelopeAndSavesData() {
		when(errandServiceMock.createErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Errand.class))).thenReturn(ERRAND_ID);

		// Client sends a mismatched applicationType — the slug must win.
		final var request = CreateFinancialAssistanceRequest.create()
			.withTitle("Min ansökan")
			.withData(FinancialAssistanceData.create().withApplicationType("SUPPLEMENTARY"));

		final var result = service.create(MUNICIPALITY_ID, NAMESPACE, SLUG_NEW, request, null);

		assertThat(result).isEqualTo(ERRAND_ID);

		final ArgumentCaptor<Errand> errandCaptor = ArgumentCaptor.forClass(Errand.class);
		verify(errandServiceMock).createErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), errandCaptor.capture());
		assertThat(errandCaptor.getValue().getTypeSlug()).isEqualTo(SLUG_NEW);
		assertThat(errandCaptor.getValue().getStatus()).isEqualTo(STATUS_INKOMMEN);
		assertThat(errandCaptor.getValue().getTitle()).isEqualTo("Min ansökan");

		final ArgumentCaptor<FinancialAssistanceEntity> entityCaptor = ArgumentCaptor.forClass(FinancialAssistanceEntity.class);
		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(entityCaptor.getValue().getApplicationType()).isEqualTo("NEW"); // derived from SLUG_NEW, not the client value

		verify(attachmentServiceMock, never()).storeAndCombine(any(), any(), any(), any()); // no attachments supplied
		verify(stakeholderServiceMock, never()).create(any(), any(), any(), any()); // no persons → no stakeholders
	}

	@Test
	void createSavesStakeholdersFromPersons() {
		when(errandServiceMock.createErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Errand.class))).thenReturn(ERRAND_ID);

		final var request = CreateFinancialAssistanceRequest.create()
			.withTitle("Återansökan")
			.withData(FinancialAssistanceData.create().withPersons(List.of(
				Person.create().withRole("APPLICANT").withPartyId(APPLICANT_PARTY_ID).withEmail("anna@example.com").withPhone("+46701234567"),
				Person.create().withRole("CO_APPLICANT").withPartyId(CO_APPLICANT_PARTY_ID),
				Person.create().withEmail("noone@example.com")))); // no role → skipped

		service.create(MUNICIPALITY_ID, NAMESPACE, SLUG_RENEWAL, request, null);

		final ArgumentCaptor<Stakeholder> stakeholderCaptor = ArgumentCaptor.forClass(Stakeholder.class);
		verify(stakeholderServiceMock, times(2)).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), stakeholderCaptor.capture());

		final var applicant = stakeholderCaptor.getAllValues().getFirst();
		assertThat(applicant.getRole()).isEqualTo("APPLICANT");
		assertThat(applicant.getExternalId()).isEqualTo(APPLICANT_PARTY_ID);
		assertThat(applicant.getExternalIdType()).isEqualTo("PRIVATE");
		assertThat(applicant.getContactChannels()).extracting(ContactChannel::getKey, ContactChannel::getValue)
			.containsExactly(tuple("EMAIL", "anna@example.com"), tuple("PHONE", "+46701234567"));

		final var coApplicant = stakeholderCaptor.getAllValues().get(1);
		assertThat(coApplicant.getRole()).isEqualTo("CO_APPLICANT");
		assertThat(coApplicant.getExternalId()).isEqualTo(CO_APPLICANT_PARTY_ID);
		assertThat(coApplicant.getExternalIdType()).isEqualTo("PRIVATE");
		assertThat(coApplicant.getContactChannels()).isEmpty();
	}

	@Test
	void createDerivesApplicationTypeFromSlug() {
		when(errandServiceMock.createErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Errand.class))).thenReturn(ERRAND_ID);

		final var request = CreateFinancialAssistanceRequest.create().withTitle("Återansökan").withData(FinancialAssistanceData.create());

		// Empty attachment list must be treated the same as none — no combine.
		service.create(MUNICIPALITY_ID, NAMESPACE, SLUG_RENEWAL, request, List.of());

		final ArgumentCaptor<Errand> errandCaptor = ArgumentCaptor.forClass(Errand.class);
		verify(errandServiceMock).createErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), errandCaptor.capture());
		assertThat(errandCaptor.getValue().getTypeSlug()).isEqualTo(SLUG_RENEWAL);

		final ArgumentCaptor<FinancialAssistanceEntity> entityCaptor = ArgumentCaptor.forClass(FinancialAssistanceEntity.class);
		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getApplicationType()).isEqualTo("RENEWAL");

		verify(attachmentServiceMock, never()).storeAndCombine(any(), any(), any(), any());
	}

	@Test
	void createDefaultsTitleWhenMissing() {
		when(errandServiceMock.createErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Errand.class))).thenReturn(ERRAND_ID);

		final var request = CreateFinancialAssistanceRequest.create()
			.withData(FinancialAssistanceData.create().withApplicationType("NEW"));

		service.create(MUNICIPALITY_ID, NAMESPACE, SLUG_NEW, request, null);

		final ArgumentCaptor<Errand> errandCaptor = ArgumentCaptor.forClass(Errand.class);
		verify(errandServiceMock).createErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), errandCaptor.capture());
		assertThat(errandCaptor.getValue().getTitle()).isEqualTo("Ekonomiskt bistånd");
	}

	@Test
	void createWithAttachmentsStoresThemAndCombines() {
		when(errandServiceMock.createErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Errand.class))).thenReturn(ERRAND_ID);

		final var request = CreateFinancialAssistanceRequest.create().withTitle("Med bilagor").withData(FinancialAssistanceData.create());
		final List<MultipartFile> attachments = List.of(
			new MockMultipartFile("attachments", "hyreskontrakt.pdf", "application/pdf", "%PDF-1.4".getBytes()),
			new MockMultipartFile("attachments", "hyresavi.png", "image/png", new byte[] {
				1, 2, 3
			}));

		final var result = service.create(MUNICIPALITY_ID, NAMESPACE, SLUG_NEW, request, attachments);

		assertThat(result).isEqualTo(ERRAND_ID);
		verify(repositoryMock).save(any(FinancialAssistanceEntity.class));
		verify(attachmentServiceMock).storeAndCombine(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, attachments);
	}

	@Test
	void readAssemblesView() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Errand.create().withId(ERRAND_ID).withTypeSlug(SLUG_NEW).withStatus("INKOMMEN"));
		when(repositoryMock.findByErrandId(ERRAND_ID))
			.thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withApplicationType("NEW")));
		final var approvals = SectionApprovals.create().withCalculation(SectionApproval.create().withSection("CALCULATION").withApproved(true));
		when(sectionApprovalServiceMock.approvals(ERRAND_ID)).thenReturn(approvals);

		final var view = service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(view.getId()).isEqualTo(ERRAND_ID);
		assertThat(view.getData()).isNotNull();
		assertThat(view.getData().getApplicationType()).isEqualTo("NEW");
		assertThat(view.getSectionApprovals()).isSameAs(approvals);
	}

	@Test
	void readPopulatesRecommendationFromLatestRecommendationDecision() {
		final var recommendation = Decision.create().withDecisionType("RECOMMENDATION").withValue("OK");
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Errand.create().withId(ERRAND_ID).withTypeSlug(SLUG_NEW).withStatus("INKOMMEN"));
		when(repositoryMock.findByErrandId(ERRAND_ID))
			.thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withApplicationType("NEW")));
		when(decisionServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(List.of(recommendation, Decision.create().withDecisionType("ACTUALISATION").withValue("4711")));

		final var view = service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(view.getRecommendation()).isEqualTo(recommendation);
	}

	@Test
	void readWithoutDataReturnsViewWithNullData() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Errand.create().withId(ERRAND_ID));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.empty());

		final var view = service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(view.getId()).isEqualTo(ERRAND_ID);
		assertThat(view.getData()).isNull();
	}

	@Test
	void readPropagatesNotFound() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenThrow(Problem.valueOf(NOT_FOUND, "x"));

		assertThatThrownBy(() -> service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(repositoryMock, never()).findByErrandId(any());
	}

	@Test
	void updateDataSavesWhenErrandExists() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Errand.create().withId(ERRAND_ID));

		service.updateData(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			FinancialAssistanceData.create().withApplicationType("RENEWAL"));

		final ArgumentCaptor<FinancialAssistanceEntity> entityCaptor = ArgumentCaptor.forClass(FinancialAssistanceEntity.class);
		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getApplicationType()).isEqualTo("RENEWAL");
	}

	@Test
	void commitPostsEffectiveRowsAndReturnsId() {
		final var month = YearMonth.of(2026, 6);
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(draftServiceMock.header(ERRAND_ID)).thenReturn(Optional.of(FaNormberakningDraftEntity.create().withErrandId(ERRAND_ID).withNormId(7)));
		when(draftServiceMock.liveIncomes(ERRAND_ID)).thenReturn(List.of(
			FaNormIncomeEntity.create().withTypeId(20).withApplicantProcessAmount(new BigDecimal("1000")).withApplicantHandlaggareAmount(new BigDecimal("1100"))));
		when(draftServiceMock.liveExpenses(ERRAND_ID)).thenReturn(List.of(FaNormExpenseEntity.create().withCostType("RENT").withAppliedAmount(new BigDecimal("9000")).withProcessAmount(new BigDecimal("8000"))));
		when(draftServiceMock.livePersons(ERRAND_ID)).thenReturn(List.of(FaNormPersonEntity.create().withPartyId("p1").withProcessDays(30)));
		when(normberakningServiceMock.commitEffective(eq("199001011234"), eq(month), any(NormberakningHeader.class), any(), any(), any())).thenReturn(4712);

		final var request = NormberakningRequest.create()
			.withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID)
			.withUnhandledIncomes(List.of("Något (EJ_PA_LISTAN)")).withChangeWarnings(List.of("Bostadsbidrag: -23%"));

		final var response = service.commitNormberakning(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.getCalculationId()).isEqualTo(4712);
		assertThat(response.getUnhandledIncomes()).containsExactly("Något (EJ_PA_LISTAN)");
		assertThat(response.getChangeWarnings()).containsExactly("Bostadsbidrag: -23%");

		final ArgumentCaptor<List<EffectiveIncome>> incomeCaptor = ArgumentCaptor.captor();
		verify(normberakningServiceMock).commitEffective(eq("199001011234"), eq(month), any(NormberakningHeader.class), incomeCaptor.capture(), any(), any());
		assertThat(incomeCaptor.getValue()).singleElement().satisfies(income -> {
			assertThat(income.typeId()).isEqualTo(20);
			assertThat(income.applicantAmount()).isEqualTo(1100.0); // handläggare value wins over the process value
		});
		// commit does not touch the errand status/recommendation — that is prepare's job
		verifyNoInteractions(decisionServiceMock);
	}

	@Test
	void commitRequiresErrandId() {
		final var request = NormberakningRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06");

		assertThatThrownBy(() -> service.commitNormberakning(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class).hasFieldOrPropertyWithValue("status", BAD_REQUEST);
	}

	@Test
	void commitYields404WhenNoDraftHeader() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(draftServiceMock.header(ERRAND_ID)).thenReturn(Optional.empty());
		final var request = NormberakningRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID);

		assertThatThrownBy(() -> service.commitNormberakning(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class).hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void prepareRequiresErrandId() {
		final var request = NormberakningRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withClassifiedIncomes("[]");

		assertThatThrownBy(() -> service.prepareNormberakning(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class).hasFieldOrPropertyWithValue("status", BAD_REQUEST);
	}

	@Test
	void prepareUnresolvedPartyIdYields404() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.empty());
		final var request = NormberakningRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[]");

		assertThatThrownBy(() -> service.prepareNormberakning(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class).hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void prepareRequiresClassifiedIncomes() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		final var request = NormberakningRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID);

		assertThatThrownBy(() -> service.prepareNormberakning(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class).hasFieldOrPropertyWithValue("status", BAD_REQUEST);
	}

	@Test
	void prepareMissingErrandYields404() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.empty());
		final var request = NormberakningRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[]");

		assertThatThrownBy(() -> service.prepareNormberakning(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class).hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void prepareRecordsReviewRequiredRecommendationAndKompletteringWhenIncomplete() {
		final var month = YearMonth.of(2026, 6);
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withNormType("RIKSNORM")));
		when(normberakningServiceMock.completeness("199001011234", month, "[json]")).thenReturn(new Completeness(false, List.of("Dagersättning")));
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withStatus("UNDER_BEREDNING"));
		when(normberakningFeederMock.expenseFeed(eq(MUNICIPALITY_ID), eq(ERRAND_ID), any())).thenReturn(new NormberakningFeeder.ExpenseFeed(List.of(), List.of()));

		final var request = NormberakningRequest.create()
			.withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[json]")
			.withUnhandledIncomes(List.of("Bostadstillägg (NOT_ON_WHITELIST)")).withChangeWarnings(List.of("Bostadsbidrag: -23%"));

		final var response = service.prepareNormberakning(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.getCalculationId()).isNull();
		assertThat(response.isInformationComplete()).isFalse();
		assertThat(response.getMissingIncomeTypes()).containsExactly("Dagersättning");

		final var decisionCaptor = ArgumentCaptor.forClass(Decision.class);
		verify(decisionServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), decisionCaptor.capture());
		final var decision = decisionCaptor.getValue();
		assertThat(decision.getDecisionType()).isEqualTo("RECOMMENDATION");
		assertThat(decision.getValue()).isEqualTo("REVIEW_REQUIRED");
		assertThat(decision.getCreatedBy()).isEqualTo("drakel");
		assertThat(decision.getDescription())
			.contains("Ej överförd inkomst: Bostadstillägg (NOT_ON_WHITELIST)")
			.contains("Saknas ännu i SSBTEK: Dagersättning");

		final var patchCaptor = ArgumentCaptor.forClass(PatchErrand.class);
		verify(errandServiceMock).updateErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchCaptor.capture());
		assertThat(patchCaptor.getValue().getStatus()).isEqualTo("KOMPLETTERING");
		verify(warningServiceMock).reconcileNormberakningWarnings(eq(ERRAND_ID),
			eq(List.of("Bostadstillägg (NOT_ON_WHITELIST)")), eq(List.of("Bostadsbidrag: -23%")), eq(List.of("Dagersättning")), any(), any(), any());
	}

	@Test
	void prepareRecordsOkRecommendationAndVantarWhenComplete() {
		final var month = YearMonth.of(2026, 6);
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID)));
		when(normberakningServiceMock.completeness("199001011234", month, "[]")).thenReturn(new Completeness(true, List.of()));
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withStatus("KOMPLETTERING"));
		when(normberakningFeederMock.expenseFeed(eq(MUNICIPALITY_ID), eq(ERRAND_ID), any())).thenReturn(new NormberakningFeeder.ExpenseFeed(List.of(), List.of()));

		final var request = NormberakningRequest.create()
			.withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[]");

		service.prepareNormberakning(MUNICIPALITY_ID, NAMESPACE, request);

		final var decisionCaptor = ArgumentCaptor.forClass(Decision.class);
		verify(decisionServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), decisionCaptor.capture());
		assertThat(decisionCaptor.getValue().getValue()).isEqualTo("OK");
		assertThat(decisionCaptor.getValue().getDescription()).contains("Inga varningar");

		final var patchCaptor = ArgumentCaptor.forClass(PatchErrand.class);
		verify(errandServiceMock).updateErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchCaptor.capture());
		assertThat(patchCaptor.getValue().getStatus()).isEqualTo("VANTAR_PA_BESLUT");
	}

	@Test
	void prepareDoesNotDuplicateRecommendationOrRewriteUnchangedStatus() {
		final var month = YearMonth.of(2026, 6);
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID)));
		when(normberakningServiceMock.completeness("199001011234", month, "[]")).thenReturn(new Completeness(true, List.of()));
		// a recommendation already exists, and the errand is already in the target status
		when(decisionServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(List.of(Decision.create().withDecisionType("RECOMMENDATION")));
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withStatus("VANTAR_PA_BESLUT"));
		when(normberakningFeederMock.expenseFeed(eq(MUNICIPALITY_ID), eq(ERRAND_ID), any())).thenReturn(new NormberakningFeeder.ExpenseFeed(List.of(), List.of()));

		final var request = NormberakningRequest.create()
			.withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[]");

		service.prepareNormberakning(MUNICIPALITY_ID, NAMESPACE, request);

		verify(decisionServiceMock, never()).create(any(), any(), any(), any());
		verify(errandServiceMock, never()).updateErrand(any(), any(), any(), any());
	}

	@Test
	void getDraftReturnsDraftAfterScopeCheck() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID));
		final var draft = NormberakningDraft.create().withApplicationMonth("2026-06");
		when(draftServiceMock.get(ERRAND_ID)).thenReturn(draft);

		assertThat(service.getDraft(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isSameAs(draft);
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void patchDraftHeaderScopeChecksThenDelegates() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID));
		final var draft = NormberakningDraft.create().withNormId(5);
		when(draftServiceMock.patchHeader(eq(ERRAND_ID), any(NormHeaderInput.class))).thenReturn(draft);

		assertThat(service.patchDraftHeader(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new NormHeaderInput().withHouseholdSize(1))).isSameAs(draft);
		verify(draftServiceMock).patchHeader(eq(ERRAND_ID), any(NormHeaderInput.class));
	}

	@Test
	void perRowDraftEditsScopeCheckThenDelegate() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID));
		final var row = NormIncomeRow.create().withId("r1");
		final var input = new NormIncomeInput().withTypeId(20);
		when(draftServiceMock.addIncome(eq(ERRAND_ID), any())).thenReturn(row);
		when(draftServiceMock.patchIncome(eq(ERRAND_ID), eq("r1"), any())).thenReturn(row);
		when(draftServiceMock.setIncomeDeleted(ERRAND_ID, "r1", true)).thenReturn(row);
		when(draftServiceMock.setIncomeDeleted(ERRAND_ID, "r1", false)).thenReturn(row);

		assertThat(service.addDraftIncome(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, input)).isSameAs(row);
		assertThat(service.patchDraftIncome(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "r1", input)).isSameAs(row);
		assertThat(service.setDraftIncomeDeleted(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "r1", true)).isSameAs(row);
		assertThat(service.setDraftIncomeDeleted(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "r1", false)).isSameAs(row);
		verify(errandServiceMock, times(4)).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void listWarningsReturnsWarningsAfterScopeCheck() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID));
		final var warnings = List.of(Warning.create().withId("w-1").withType("NEW_INCOME").withStatus("UNHANDLED"));
		when(warningServiceMock.list(ERRAND_ID)).thenReturn(warnings);

		assertThat(service.listWarnings(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isEqualTo(warnings);
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void updateWarningDelegatesAfterScopeCheck() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID));
		final var warning = Warning.create().withId("w-1").withStatus("ACKNOWLEDGED");
		when(warningServiceMock.updateStatus(ERRAND_ID, "w-1", "ACKNOWLEDGED")).thenReturn(warning);

		assertThat(service.updateWarning(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "w-1", "ACKNOWLEDGED")).isSameAs(warning);
		verify(warningServiceMock).updateStatus(ERRAND_ID, "w-1", "ACKNOWLEDGED");
	}

	@Test
	void getSectionApprovalsReturnsBundleAfterScopeCheck() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID));
		final var approvals = SectionApprovals.create().withPayment(SectionApproval.create().withSection("PAYMENT").withApproved(false));
		when(sectionApprovalServiceMock.approvals(ERRAND_ID)).thenReturn(approvals);

		assertThat(service.getSectionApprovals(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isSameAs(approvals);
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void setSectionApprovalDelegatesAfterScopeCheck() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID));
		final var approval = SectionApproval.create().withSection("DECISION").withApproved(true).withApprovedBy("jane02doe");
		when(sectionApprovalServiceMock.setApproval(ERRAND_ID, "DECISION", true, "jane02doe")).thenReturn(approval);

		assertThat(service.setSectionApproval(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "DECISION", true, "jane02doe")).isSameAs(approval);
		verify(sectionApprovalServiceMock).setApproval(ERRAND_ID, "DECISION", true, "jane02doe");
	}

	@Test
	void createActualisationResolvesPartyDelegatesAndMaps() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(actualisationServiceMock.create("199001011234", LocalDate.of(2026, 6, 1))).thenReturn(5012);

		final var request = ActualisationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID)
			.withApplicationMonth("2026-06");

		final var response = service.createActualisation(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.getActualisationId()).isEqualTo(5012);
		verify(actualisationServiceMock).create("199001011234", LocalDate.of(2026, 6, 1));
		// No errandId on the request → nothing recorded on an errand.
		verify(decisionServiceMock, never()).create(any(), any(), any(), any());
	}

	@Test
	void createActualisationWithErrandIdRecordsActualisationDecision() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(actualisationServiceMock.create("199001011234", LocalDate.of(2026, 6, 1))).thenReturn(5012);

		final var request = ActualisationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID)
			.withApplicationMonth("2026-06")
			.withErrandId(ERRAND_ID);

		final var response = service.createActualisation(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.getActualisationId()).isEqualTo(5012);

		final var decisionCaptor = ArgumentCaptor.forClass(Decision.class);
		verify(decisionServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), decisionCaptor.capture());
		final var decision = decisionCaptor.getValue();
		assertThat(decision.getDecisionType()).isEqualTo("ACTUALISATION");
		assertThat(decision.getValue()).isEqualTo("5012");
		assertThat(decision.getCreatedBy()).isEqualTo("drakel");
		assertThat(decision.getDescription()).contains("id 5012");
	}

	@Test
	void createActualisationUnresolvedPartyIdYields404() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.empty());

		final var request = ActualisationRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06");

		assertThatThrownBy(() -> service.createActualisation(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(actualisationServiceMock, never()).create(any(), any());
		verify(decisionServiceMock, never()).create(any(), any(), any(), any());
	}

	@Test
	void checkPaymentStatusEffectuated() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(paymentStatusServiceMock.read("199001011234", YearMonth.of(2026, 6))).thenReturn(new PaymentStatus(true, "2026-05-27"));

		final var request = PaymentStatusRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06");

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, request);

		assertThat(response.getEffectuated()).isTrue();
		assertThat(response.getPaymentDate()).isEqualTo("2026-05-27");
		verify(paymentStatusServiceMock).read("199001011234", YearMonth.of(2026, 6));
	}

	@Test
	void checkPaymentStatusNotEffectuated() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(paymentStatusServiceMock.read("199001011234", YearMonth.of(2026, 6))).thenReturn(new PaymentStatus(false, null));

		final var request = PaymentStatusRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06");

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, request);

		assertThat(response.getEffectuated()).isFalse();
		assertThat(response.getPaymentDate()).isNull();
	}

	@Test
	void checkPaymentStatusUnresolvedPartyIdYields404() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.empty());

		final var request = PaymentStatusRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06");

		assertThatThrownBy(() -> service.checkPaymentStatus(MUNICIPALITY_ID, request))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(paymentStatusServiceMock, never()).read(any(), any());
	}
}
