package se.sundsvall.caremanagement.types.financialassistance.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
import se.sundsvall.caremanagement.lifecare.service.ActualisationResult;
import se.sundsvall.caremanagement.lifecare.service.ActualisationService;
import se.sundsvall.caremanagement.lifecare.service.CalculationService;
import se.sundsvall.caremanagement.lifecare.service.LifecareEbCaseService;
import se.sundsvall.caremanagement.lifecare.service.PaymentStatus;
import se.sundsvall.caremanagement.lifecare.service.PaymentStatusService;
import se.sundsvall.caremanagement.lifecare.service.model.ActualisationSummary;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicationIncome;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationExpenseView;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationHeader;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationIncomeView;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationPersonView;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationView;
import se.sundsvall.caremanagement.lifecare.service.model.Completeness;
import se.sundsvall.caremanagement.lifecare.service.model.DecisionPersonView;
import se.sundsvall.caremanagement.lifecare.service.model.DecisionView;
import se.sundsvall.caremanagement.lifecare.service.model.DocumentView;
import se.sundsvall.caremanagement.lifecare.service.model.EffectiveIncome;
import se.sundsvall.caremanagement.lifecare.service.model.FcIncomeLine;
import se.sundsvall.caremanagement.stakeholders.api.model.ContactChannel;
import se.sundsvall.caremanagement.stakeholders.api.model.Stakeholder;
import se.sundsvall.caremanagement.stakeholders.service.StakeholderService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ActualisationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ArchiveActualisationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateFinancialAssistanceRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceData;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormHeaderInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Person;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApproval;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApprovals;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaCalculationDraftEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaIncome;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormExpenseEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormIncomeEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormPersonEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.time.Month.JANUARY;
import static java.time.Month.JUNE;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_NEW;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_RENEWAL;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_RECEIVED;

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
	private CalculationService calculationServiceMock;

	@Mock
	private LifecareEbCaseService lifecareEbCaseServiceMock;

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
	private CalculationFeeder calculationFeederMock;

	@Mock
	private se.sundsvall.caremanagement.rpa.service.RpaService rpaServiceMock;

	@Mock
	private se.sundsvall.caremanagement.formsnapshot.service.FormSnapshotService formSnapshotServiceMock;

	@Mock
	private se.sundsvall.caremanagement.lifecare.service.LifecareCaseHistoryService lifecareCaseHistoryServiceMock;

	@InjectMocks
	private FinancialAssistanceService service;

	private static final String APPLICANT_PARTY_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
	private static final String CO_APPLICANT_PARTY_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";

	@Test
	void createBuildsEnvelopeAndSavesData() {
		when(errandServiceMock.createTypedErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Errand.class))).thenReturn(ERRAND_ID);

		// Client sends a mismatched applicationType — the slug must win.
		final var request = CreateFinancialAssistanceRequest.create()
			.withTitle("Min application")
			.withData(FinancialAssistanceData.create().withApplicationType("SUPPLEMENTARY"));

		final var result = service.create(MUNICIPALITY_ID, NAMESPACE, SLUG_NEW, request, null, null, null);

		assertThat(result).isEqualTo(ERRAND_ID);

		final ArgumentCaptor<Errand> errandCaptor = ArgumentCaptor.forClass(Errand.class);
		verify(errandServiceMock).createTypedErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), errandCaptor.capture());
		assertThat(errandCaptor.getValue().getTypeSlug()).isEqualTo(SLUG_NEW);
		assertThat(errandCaptor.getValue().getStatus()).isEqualTo(STATUS_RECEIVED);
		assertThat(errandCaptor.getValue().getTitle()).isEqualTo("Min application");

		final ArgumentCaptor<FinancialAssistanceEntity> entityCaptor = ArgumentCaptor.forClass(FinancialAssistanceEntity.class);
		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(entityCaptor.getValue().getApplicationType()).isEqualTo("NEW"); // derived from SLUG_NEW, not the client value

		verify(attachmentServiceMock, never()).storeAndCombine(any(), any(), any(), any()); // no attachments supplied
		verify(stakeholderServiceMock, never()).create(any(), any(), any(), any()); // no persons → no stakeholders
	}

	@Test
	void createSavesStakeholdersFromPersons() {
		when(errandServiceMock.createTypedErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Errand.class))).thenReturn(ERRAND_ID);

		final var request = CreateFinancialAssistanceRequest.create()
			.withTitle("Renewal")
			.withData(FinancialAssistanceData.create().withPersons(List.of(
				Person.create().withRole("APPLICANT").withPartyId(APPLICANT_PARTY_ID).withEmail("anna@example.com").withPhone("+46701234567"),
				Person.create().withRole("CO_APPLICANT").withPartyId(CO_APPLICANT_PARTY_ID),
				Person.create().withEmail("noone@example.com")))); // no role → skipped

		service.create(MUNICIPALITY_ID, NAMESPACE, SLUG_RENEWAL, request, null, null, null);

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
		when(errandServiceMock.createTypedErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Errand.class))).thenReturn(ERRAND_ID);

		final var request = CreateFinancialAssistanceRequest.create().withTitle("Renewal").withData(FinancialAssistanceData.create());

		// Empty attachment list must be treated the same as none — no combine.
		service.create(MUNICIPALITY_ID, NAMESPACE, SLUG_RENEWAL, request, List.of(), null, null);

		final ArgumentCaptor<Errand> errandCaptor = ArgumentCaptor.forClass(Errand.class);
		verify(errandServiceMock).createTypedErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), errandCaptor.capture());
		assertThat(errandCaptor.getValue().getTypeSlug()).isEqualTo(SLUG_RENEWAL);

		final ArgumentCaptor<FinancialAssistanceEntity> entityCaptor = ArgumentCaptor.forClass(FinancialAssistanceEntity.class);
		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getApplicationType()).isEqualTo("RENEWAL");

		verify(attachmentServiceMock, never()).storeAndCombine(any(), any(), any(), any());
	}

	@Test
	void createDefaultsTitleWhenMissing() {
		when(errandServiceMock.createTypedErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Errand.class))).thenReturn(ERRAND_ID);

		final var request = CreateFinancialAssistanceRequest.create()
			.withData(FinancialAssistanceData.create().withApplicationType("NEW"));

		service.create(MUNICIPALITY_ID, NAMESPACE, SLUG_NEW, request, null, null, null);

		final ArgumentCaptor<Errand> errandCaptor = ArgumentCaptor.forClass(Errand.class);
		verify(errandServiceMock).createTypedErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), errandCaptor.capture());
		assertThat(errandCaptor.getValue().getTitle()).isEqualTo("Financial assistance");
	}

	@Test
	void createWithAttachmentsStoresThemAndCombines() {
		when(errandServiceMock.createTypedErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Errand.class))).thenReturn(ERRAND_ID);

		final var request = CreateFinancialAssistanceRequest.create().withTitle("Med bilagor").withData(FinancialAssistanceData.create());
		final List<MultipartFile> attachments = List.of(
			new MockMultipartFile("attachments", "hyreskontrakt.pdf", "application/pdf", "%PDF-1.4".getBytes()),
			new MockMultipartFile("attachments", "hyresavi.png", "image/png", new byte[] {
				1, 2, 3
			}));

		final var result = service.create(MUNICIPALITY_ID, NAMESPACE, SLUG_NEW, request, attachments, null, null);

		assertThat(result).isEqualTo(ERRAND_ID);
		verify(repositoryMock).save(any(FinancialAssistanceEntity.class));
		verify(attachmentServiceMock).storeAndCombine(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, attachments);
	}

	@Test
	void createWithCaseDataStoresSnapshot() {
		when(errandServiceMock.createTypedErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Errand.class))).thenReturn(ERRAND_ID);

		final var request = CreateFinancialAssistanceRequest.create().withTitle("Med ärendeuppgifter").withData(FinancialAssistanceData.create());
		final var caseData = new MockMultipartFile("caseData", "snapshot.pdf", "application/pdf", "%PDF-1.4".getBytes());

		final var result = service.create(MUNICIPALITY_ID, NAMESPACE, SLUG_NEW, request, null, caseData, null);

		assertThat(result).isEqualTo(ERRAND_ID);
		verify(attachmentServiceMock).createCaseDataAttachment(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, caseData);
		verify(attachmentServiceMock, never()).storeAndCombine(any(), any(), any(), any()); // no supporting files supplied
		verifyNoInteractions(formSnapshotServiceMock); // no form snapshot supplied
	}

	@Test
	void createWithFormSnapshotCapturesIt() {
		when(errandServiceMock.createTypedErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Errand.class))).thenReturn(ERRAND_ID);

		final var request = CreateFinancialAssistanceRequest.create().withTitle("Med formulärsnapshot").withData(FinancialAssistanceData.create());
		final var payload = "{\"schemaVersion\":\"form-snapshot/1\",\"sections\":[]}";

		final var result = service.create(MUNICIPALITY_ID, NAMESPACE, SLUG_NEW, request, null, null, payload);

		assertThat(result).isEqualTo(ERRAND_ID);
		verify(formSnapshotServiceMock).saveErrandFormSnapshot(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, SLUG_NEW, payload);
	}

	@Test
	void createWithBlankFormSnapshotSkipsCapture() {
		when(errandServiceMock.createTypedErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Errand.class))).thenReturn(ERRAND_ID);

		final var request = CreateFinancialAssistanceRequest.create().withData(FinancialAssistanceData.create());

		service.create(MUNICIPALITY_ID, NAMESPACE, SLUG_NEW, request, null, null, "  ");

		verifyNoInteractions(formSnapshotServiceMock); // blank payload → no capture
	}

	@Test
	void readFormSnapshotScopesAndDelegates() {
		final var snapshot = se.sundsvall.caremanagement.formsnapshot.api.model.FormSnapshot.create().withSchemaVersion("form-snapshot/1");
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Errand.create().withId(ERRAND_ID).withTypeSlug(SLUG_NEW).withStatus("RECEIVED"));
		when(formSnapshotServiceMock.readErrandFormSnapshot(ERRAND_ID)).thenReturn(snapshot);

		final var result = service.readFormSnapshot(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(result).isSameAs(snapshot);
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID); // scope check
		verify(formSnapshotServiceMock).readErrandFormSnapshot(ERRAND_ID);
	}

	@Test
	void readAssemblesView() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Errand.create().withId(ERRAND_ID).withTypeSlug(SLUG_NEW).withStatus("RECEIVED"));
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
			.thenReturn(Errand.create().withId(ERRAND_ID).withTypeSlug(SLUG_NEW).withStatus("RECEIVED"));
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
		final var month = YearMonth.of(2026, JUNE);
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(draftServiceMock.header(ERRAND_ID)).thenReturn(Optional.of(FaCalculationDraftEntity.create().withErrandId(ERRAND_ID).withNormId(7)));
		when(draftServiceMock.liveIncomes(ERRAND_ID)).thenReturn(List.of(
			FaNormIncomeEntity.create().withTypeId(20).withApplicantProcessAmount(new BigDecimal("1000")).withApplicantCaseworkerAmount(new BigDecimal("1100"))));
		when(draftServiceMock.liveExpenses(ERRAND_ID)).thenReturn(List.of(FaNormExpenseEntity.create().withCostType("RENT").withAppliedAmount(new BigDecimal("9000")).withProcessAmount(new BigDecimal("8000"))));
		when(draftServiceMock.livePersons(ERRAND_ID)).thenReturn(List.of(FaNormPersonEntity.create().withPartyId("p1").withProcessDays(30)));
		when(calculationServiceMock.commitEffective(eq("199001011234"), eq(month), any(CalculationHeader.class), any(), any(), any())).thenReturn(4712);

		final var request = CalculationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID)
			.withUnhandledIncomes(List.of("Något (EJ_PA_LISTAN)")).withChangeWarnings(List.of("Bostadsbidrag: -23%"));

		final var response = service.commitCalculation(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.getCalculationId()).isEqualTo(4712);
		assertThat(response.getUnhandledIncomes()).containsExactly("Något (EJ_PA_LISTAN)");
		assertThat(response.getChangeWarnings()).containsExactly("Bostadsbidrag: -23%");

		final ArgumentCaptor<List<EffectiveIncome>> incomeCaptor = ArgumentCaptor.captor();
		verify(calculationServiceMock).commitEffective(eq("199001011234"), eq(month), any(CalculationHeader.class), incomeCaptor.capture(), any(), any());
		assertThat(incomeCaptor.getValue()).singleElement().satisfies(income -> {
			assertThat(income.typeId()).isEqualTo(20);
			assertThat(income.applicantAmount()).isEqualTo(1100.0); // caseworker value wins over the process value
		});
		// commit does not touch the errand status/recommendation — that is prepare's job
		verifyNoInteractions(decisionServiceMock);
	}

	@Test
	void commitRequiresErrandId() {
		final var request = CalculationRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06");

		assertThatThrownBy(() -> service.commitCalculation(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class).hasFieldOrPropertyWithValue("status", BAD_REQUEST);
	}

	@Test
	void commitYields404WhenNoDraftHeader() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(draftServiceMock.header(ERRAND_ID)).thenReturn(Optional.empty());
		final var request = CalculationRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID);

		assertThatThrownBy(() -> service.commitCalculation(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class).hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void commitFromApplicationFeedsApplicationDataThroughTheSamePipeline() {
		final var month = YearMonth.of(2026, JUNE);
		final var errand = FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withIncomes(List.of(
			FaIncome.create().withIncomeType("SALARY").withAmount(new BigDecimal("18500")).withRecipient("APPLICANT"),
			FaIncome.create().withIncomeType("SWISH_DEPOSITS").withAmount(new BigDecimal("300")).withRecipient("CO_APPLICANT")));

		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(errand));
		when(calculationServiceMock.applicationIncomeLines(eq("199001011234"), any()))
			.thenReturn(List.of(new FcIncomeLine(11, "Lön efter skatt", "APPLICANT", new BigDecimal("18500"), null, "Ansökan")));
		when(calculationFeederMock.incomeRows(eq(ERRAND_ID), any())).thenReturn(List.of(
			FaNormIncomeEntity.create().withTypeId(11).withApplicantProcessAmount(new BigDecimal("18500"))));
		when(calculationFeederMock.applicationExpenseRows(eq(ERRAND_ID), any())).thenReturn(
			List.of(FaNormExpenseEntity.create().withCostType("RENT").withAppliedAmount(new BigDecimal("9000")).withProcessAmount(new BigDecimal("8000"))));
		when(calculationFeederMock.personRows(eq(ERRAND_ID), any())).thenReturn(List.of(FaNormPersonEntity.create().withPartyId("p1").withProcessDays(30)));
		when(calculationServiceMock.selectNormId("199001011234", month)).thenReturn(7);
		when(calculationServiceMock.commitEffective(eq("199001011234"), eq(month), any(CalculationHeader.class), any(), any(), any())).thenReturn(5001);

		final var request = CalculationRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID);
		final var response = service.commitFromApplication(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.getCalculationId()).isEqualTo(5001);

		// The application's declared incomes are mapped to the neutral ApplicationIncome (recipient → role) and handed to
		// the existing income pipeline — not a parallel calculation engine.
		final ArgumentCaptor<List<ApplicationIncome>> incomeCaptor = ArgumentCaptor.captor();
		verify(calculationServiceMock).applicationIncomeLines(eq("199001011234"), incomeCaptor.capture());
		assertThat(incomeCaptor.getValue()).extracting(ApplicationIncome::incomeType, income -> income.role().name())
			.containsExactly(tuple("SALARY", "APPLICANT"), tuple("SWISH_DEPOSITS", "CO_APPLICANT"));

		final ArgumentCaptor<List<EffectiveIncome>> effectiveCaptor = ArgumentCaptor.captor();
		verify(calculationServiceMock).commitEffective(eq("199001011234"), eq(month), any(CalculationHeader.class), effectiveCaptor.capture(), any(), any());
		assertThat(effectiveCaptor.getValue()).singleElement().satisfies(income -> assertThat(income.typeId()).isEqualTo(11));
		verify(rpaServiceMock).enqueue(eq(MUNICIPALITY_ID), eq(ERRAND_ID), any());
	}

	@Test
	void commitFromApplicationRequiresErrandId() {
		final var request = CalculationRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06");

		assertThatThrownBy(() -> service.commitFromApplication(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class).hasFieldOrPropertyWithValue("status", BAD_REQUEST);
	}

	@Test
	void commitFromApplicationYields404WhenErrandMissing() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.empty());
		final var request = CalculationRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID);

		assertThatThrownBy(() -> service.commitFromApplication(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class).hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void prepareRequiresErrandId() {
		final var request = CalculationRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withClassifiedIncomes("[]");

		assertThatThrownBy(() -> service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class).hasFieldOrPropertyWithValue("status", BAD_REQUEST);
	}

	@Test
	void prepareUnresolvedPartyIdYields404() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.empty());
		final var request = CalculationRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[]");

		assertThatThrownBy(() -> service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class).hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void prepareRequiresClassifiedIncomes() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		final var request = CalculationRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID);

		assertThatThrownBy(() -> service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class).hasFieldOrPropertyWithValue("status", BAD_REQUEST);
	}

	@Test
	void prepareMissingErrandYields404() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.empty());
		final var request = CalculationRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[]");

		assertThatThrownBy(() -> service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class).hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void prepareRecordsReviewRequiredRecommendationAndKompletteringWhenIncomplete() {
		final var month = YearMonth.of(2026, JUNE);
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withNormType(List.of("NATIONAL_NORM"))));
		when(calculationServiceMock.completeness("199001011234", month, "[json]")).thenReturn(new Completeness(false, List.of("Dagersättning")));
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withStatus("UNDER_REVIEW"));
		when(calculationFeederMock.expenseFeed(eq(MUNICIPALITY_ID), eq(ERRAND_ID), any(), any(), any())).thenReturn(new CalculationFeeder.ExpenseFeed(List.of(), List.of()));

		final var request = CalculationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[json]")
			.withUnhandledIncomes(List.of("Bostadstillägg (NOT_ON_WHITELIST)")).withChangeWarnings(List.of("Bostadsbidrag: -23%"));

		final var response = service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, request);

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
			.contains("Saknas fortfarande i SSBTEK: Dagersättning");

		final var patchCaptor = ArgumentCaptor.forClass(PatchErrand.class);
		verify(errandServiceMock).updateErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchCaptor.capture());
		assertThat(patchCaptor.getValue().getStatus()).isEqualTo("SUPPLEMENT_REQUESTED");
		verify(warningServiceMock).reconcileCalculationWarnings(eq(ERRAND_ID),
			eq(List.of("Bostadstillägg (NOT_ON_WHITELIST)")), eq(List.of("Bostadsbidrag: -23%")), eq(List.of("Dagersättning")), any(), any());
	}

	@Test
	void prepareRecordsOkRecommendationAndVantarWhenComplete() {
		final var month = YearMonth.of(2026, JUNE);
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID)));
		when(calculationServiceMock.completeness("199001011234", month, "[]")).thenReturn(new Completeness(true, List.of()));
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withStatus("SUPPLEMENT_REQUESTED"));
		when(calculationFeederMock.expenseFeed(eq(MUNICIPALITY_ID), eq(ERRAND_ID), any(), any(), any())).thenReturn(new CalculationFeeder.ExpenseFeed(List.of(), List.of()));

		final var request = CalculationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[]");

		service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, request);

		final var decisionCaptor = ArgumentCaptor.forClass(Decision.class);
		verify(decisionServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), decisionCaptor.capture());
		assertThat(decisionCaptor.getValue().getValue()).isEqualTo("OK");
		assertThat(decisionCaptor.getValue().getDescription()).contains("Inga varningar");

		final var patchCaptor = ArgumentCaptor.forClass(PatchErrand.class);
		verify(errandServiceMock).updateErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchCaptor.capture());
		assertThat(patchCaptor.getValue().getStatus()).isEqualTo("AWAITING_DECISION");

		// The daily loop stamps the errand with its run time.
		final var entityCaptor = ArgumentCaptor.forClass(FinancialAssistanceEntity.class);
		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getLastDailyRunAt()).isCloseTo(OffsetDateTime.now(), within(10, SECONDS));
	}

	@Test
	void prepareDoesNotDuplicateRecommendationOrRewriteUnchangedStatus() {
		final var month = YearMonth.of(2026, JUNE);
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID)));
		when(calculationServiceMock.completeness("199001011234", month, "[]")).thenReturn(new Completeness(true, List.of()));
		// a recommendation already exists, and the errand is already in the target status
		when(decisionServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(List.of(Decision.create().withDecisionType("RECOMMENDATION")));
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withStatus("AWAITING_DECISION"));
		when(calculationFeederMock.expenseFeed(eq(MUNICIPALITY_ID), eq(ERRAND_ID), any(), any(), any())).thenReturn(new CalculationFeeder.ExpenseFeed(List.of(), List.of()));

		final var request = CalculationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[]");

		service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, request);

		verify(decisionServiceMock, never()).create(any(), any(), any(), any());
		verify(errandServiceMock, never()).updateErrand(any(), any(), any(), any());
	}

	@Test
	void getDraftReturnsDraftAfterScopeCheck() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID));
		final var draft = CalculationDraft.create().withApplicationMonth("2026-06");
		when(draftServiceMock.get(ERRAND_ID)).thenReturn(draft);

		assertThat(service.getDraft(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isSameAs(draft);
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void patchDraftHeaderScopeChecksThenDelegates() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID));
		final var draft = CalculationDraft.create().withNormId(5);
		when(draftServiceMock.patchHeader(eq(ERRAND_ID), any(NormHeaderInput.class))).thenReturn(draft);

		assertThat(service.patchDraftHeader(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new NormHeaderInput().withHouseholdSize(1))).isSameAs(draft);
		verify(draftServiceMock).patchHeader(eq(ERRAND_ID), any(NormHeaderInput.class));
	}

	@Test
	void createActualisationResolvesPartyDelegatesAndMaps() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(actualisationServiceMock.create("199001011234", LocalDate.of(2026, JUNE, 1))).thenReturn(new ActualisationResult(5012, "anna01ker"));

		final var request = ActualisationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID)
			.withApplicationMonth("2026-06");

		final var response = service.createActualisation(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.getActualisationId()).isEqualTo(5012);
		verify(actualisationServiceMock).create("199001011234", LocalDate.of(2026, JUNE, 1));
		// No errandId on the request → nothing recorded on an errand and no assignment.
		verify(decisionServiceMock, never()).create(any(), any(), any(), any());
		verify(errandServiceMock, never()).updateErrand(any(), any(), any(), any());
	}

	@Test
	void createActualisationWithErrandIdRecordsActualisationDecisionAndAssignsCaseworker() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(actualisationServiceMock.create("199001011234", LocalDate.of(2026, JUNE, 1))).thenReturn(new ActualisationResult(5012, "anna01ker"));

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

		final var patchCaptor = ArgumentCaptor.forClass(PatchErrand.class);
		verify(errandServiceMock).updateErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchCaptor.capture());
		assertThat(patchCaptor.getValue().getAssignedUserId()).isEqualTo("anna01ker");
	}

	@Test
	void createActualisationWithErrandIdButNoResolvedCaseworkerRecordsDecisionWithoutAssigning() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(actualisationServiceMock.create("199001011234", LocalDate.of(2026, JUNE, 1))).thenReturn(new ActualisationResult(5012, null));

		final var request = ActualisationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID)
			.withApplicationMonth("2026-06")
			.withErrandId(ERRAND_ID);

		final var response = service.createActualisation(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.getActualisationId()).isEqualTo(5012);
		verify(decisionServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(Decision.class));
		verify(errandServiceMock, never()).updateErrand(any(), any(), any(), any());
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
	void listActualisationsResolvesPartyDefaultsPeriodAndMaps() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		final var summary = new ActualisationSummary(5012, "Ansökan", "Ekonomiskt bistånd", "2026-06-01", "Nyansökan", "Försörjningsstöd",
			"Den enskilde", "Anna Andersson", "IFO", "Pågående", 8801, 7700, 9900);
		when(actualisationServiceMock.list(eq("199001011234"), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of(summary));

		final var result = service.listActualisations(MUNICIPALITY_ID, APPLICANT_PARTY_ID, null, null);

		assertThat(result).singleElement().satisfies(actualisation -> {
			assertThat(actualisation.getId()).isEqualTo(5012);
			assertThat(actualisation.getType()).isEqualTo("Ansökan");
			assertThat(actualisation.getName()).isEqualTo("Ekonomiskt bistånd");
			assertThat(actualisation.getDate()).isEqualTo("2026-06-01");
			assertThat(actualisation.getReason()).isEqualTo("Nyansökan");
			assertThat(actualisation.getRegards()).isEqualTo("Försörjningsstöd");
			assertThat(actualisation.getFromWho()).isEqualTo("Den enskilde");
			assertThat(actualisation.getCaseworker()).isEqualTo("Anna Andersson");
			assertThat(actualisation.getOrganization()).isEqualTo("IFO");
			assertThat(actualisation.getStatus()).isEqualTo("Pågående");
			assertThat(actualisation.getInvestigationId()).isEqualTo(8801);
			assertThat(actualisation.getServiceId()).isEqualTo(7700);
			assertThat(actualisation.getDecisionId()).isEqualTo(9900);
		});

		final var fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
		final var toCaptor = ArgumentCaptor.forClass(LocalDate.class);
		verify(actualisationServiceMock).list(eq("199001011234"), fromCaptor.capture(), toCaptor.capture());
		assertThat(toCaptor.getValue()).isEqualTo(LocalDate.now());
		assertThat(fromCaptor.getValue()).isEqualTo(toCaptor.getValue().minusMonths(24));
	}

	@Test
	void listActualisationsUsesExplicitPeriod() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(actualisationServiceMock.list("199001011234", LocalDate.of(2026, JANUARY, 1), LocalDate.of(2026, JUNE, 30))).thenReturn(List.of());

		final var result = service.listActualisations(MUNICIPALITY_ID, APPLICANT_PARTY_ID, LocalDate.of(2026, JANUARY, 1), LocalDate.of(2026, JUNE, 30));

		assertThat(result).isEmpty();
		verify(actualisationServiceMock).list("199001011234", LocalDate.of(2026, JANUARY, 1), LocalDate.of(2026, JUNE, 30));
	}

	@Test
	void listActualisationsUnresolvedPartyIdYields404() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.listActualisations(MUNICIPALITY_ID, APPLICANT_PARTY_ID, null, null))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(actualisationServiceMock, never()).list(any(), any(), any());
	}

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
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

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
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(lifecareCaseHistoryServiceMock, never()).documentContent(any());
	}

	private void applicantOwnsActualisation5012() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(actualisationServiceMock.list(eq("199001011234"), any(LocalDate.class), any(LocalDate.class)))
			.thenReturn(List.of(new ActualisationSummary(5012, "Ansökan", "EB", "2026-06-01", "Nyansökan", "Försörjningsstöd", "Den enskilde", "Anna", "IFO", "Pågående", 8801, 7700, 9900)));
	}

	@Test
	void archiveToActualisationForwardsFileWithDefaultsWhenNoMetadata() {
		applicantOwnsActualisation5012();
		final var file = new MockMultipartFile("file", "tillaggsansokan.pdf", "application/pdf", new byte[] {
			1, 2, 3
		});

		service.archiveToActualisation(MUNICIPALITY_ID, NAMESPACE, APPLICANT_PARTY_ID, 5012, file, null);

		verify(actualisationServiceMock).uploadAttachment(5012, "tillaggsansokan.pdf", new byte[] {
			1, 2, 3
		}, "ANSOKAN", "ENSKILD", "tillaggsansokan.pdf", "Draken");
		// No errandId → nothing recorded on an errand.
		verify(decisionServiceMock, never()).create(any(), any(), any(), any());
	}

	@Test
	void archiveToActualisationUsesRequestMetadataWhenProvided() {
		final var file = new MockMultipartFile("file", "tillaggsansokan.pdf", "application/pdf", new byte[] {
			9
		});
		final var request = ArchiveActualisationRequest.create()
			.withTitle("Tilläggsansökan juni")
			.withDocumentType("KOMPLETTERING")
			.withDocumentSenderType("MYNDIGHET")
			.withSenderName("Sundsvalls kommun");
		applicantOwnsActualisation5012();

		service.archiveToActualisation(MUNICIPALITY_ID, NAMESPACE, APPLICANT_PARTY_ID, 5012, file, request);

		verify(actualisationServiceMock).uploadAttachment(5012, "tillaggsansokan.pdf", new byte[] {
			9
		}, "KOMPLETTERING", "MYNDIGHET", "Tilläggsansökan juni", "Sundsvalls kommun");
		verify(decisionServiceMock, never()).create(any(), any(), any(), any());
	}

	@Test
	void archiveToActualisationRecordsActualisationDecisionWhenErrandIdPresent() {
		final var file = new MockMultipartFile("file", "tillaggsansokan.pdf", "application/pdf", new byte[] {
			7
		});
		final var request = ArchiveActualisationRequest.create().withErrandId(ERRAND_ID);
		applicantOwnsActualisation5012();

		service.archiveToActualisation(MUNICIPALITY_ID, NAMESPACE, APPLICANT_PARTY_ID, 5012, file, request);

		verify(actualisationServiceMock).uploadAttachment(eq(5012), eq("tillaggsansokan.pdf"), any(), eq("ANSOKAN"), eq("ENSKILD"), eq("tillaggsansokan.pdf"), eq("Draken"));

		final var decisionCaptor = ArgumentCaptor.forClass(Decision.class);
		verify(decisionServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), decisionCaptor.capture());
		final var decision = decisionCaptor.getValue();
		assertThat(decision.getDecisionType()).isEqualTo("ACTUALISATION");
		assertThat(decision.getValue()).isEqualTo("5012");
		assertThat(decision.getCreatedBy()).isEqualTo("drakel");
		assertThat(decision.getDescription()).contains("id 5012");
	}

	@Test
	void archiveToActualisationWrapsUnreadableFileAs400() throws IOException {
		applicantOwnsActualisation5012();
		final var file = mock(MultipartFile.class);
		when(file.getOriginalFilename()).thenReturn("tillaggsansokan.pdf");
		when(file.getBytes()).thenThrow(new IOException("stream closed"));

		assertThatThrownBy(() -> service.archiveToActualisation(MUNICIPALITY_ID, NAMESPACE, APPLICANT_PARTY_ID, 5012, file, null))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST);

		verify(actualisationServiceMock, never()).uploadAttachment(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void archiveToActualisationForeignActualisationYields404() {
		applicantOwnsActualisation5012();
		final var file = new MockMultipartFile("file", "x.pdf", "application/pdf", new byte[] {
			1
		});

		assertThatThrownBy(() -> service.archiveToActualisation(MUNICIPALITY_ID, NAMESPACE, APPLICANT_PARTY_ID, 9999, file, null))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(actualisationServiceMock, never()).uploadAttachment(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void checkPaymentStatusEffectuated() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(paymentStatusServiceMock.read("199001011234", YearMonth.of(2026, JUNE))).thenReturn(new PaymentStatus(true, "2026-05-27"));

		final var request = PaymentStatusRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06");

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, request);

		assertThat(response.getEffectuated()).isTrue();
		assertThat(response.getPaymentDate()).isEqualTo("2026-05-27");
		verify(paymentStatusServiceMock).read("199001011234", YearMonth.of(2026, JUNE));
	}

	@Test
	void checkPaymentStatusNotEffectuated() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(paymentStatusServiceMock.read("199001011234", YearMonth.of(2026, JUNE))).thenReturn(new PaymentStatus(false, null));

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
