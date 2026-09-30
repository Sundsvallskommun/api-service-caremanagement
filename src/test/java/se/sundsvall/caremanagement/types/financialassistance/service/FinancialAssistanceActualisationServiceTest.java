package se.sundsvall.caremanagement.types.financialassistance.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.IntPredicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.attachments.service.AttachmentService;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.api.model.PatchErrand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.decisions.api.model.Decision;
import se.sundsvall.caremanagement.decisions.service.DecisionService;
import se.sundsvall.caremanagement.lifecare.service.ActualisationResult;
import se.sundsvall.caremanagement.lifecare.service.ActualisationService;
import se.sundsvall.caremanagement.lifecare.service.AttachmentUpload;
import se.sundsvall.caremanagement.lifecare.service.model.ActualisationSummary;
import se.sundsvall.caremanagement.shared.SourceFile;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ActualisationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ArchiveActualisationRequest;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Month.JANUARY;
import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class FinancialAssistanceActualisationServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";
	private static final String APPLICANT_PARTY_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";

	@Mock
	private ActualisationService actualisationServiceMock;

	@Mock
	private AttachmentService attachmentServiceMock;

	@Mock
	private DecisionService decisionServiceMock;

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private FinancialAssistanceRepository financialAssistanceRepositoryMock;

	@InjectMocks
	private FinancialAssistanceActualisationService service;

	/** The errand's arrival stamp — "datum för inskickandet" — 17 June, deliberately not the 1st. */
	private static final OffsetDateTime SUBMITTED_AT = OffsetDateTime.of(2026, 6, 17, 9, 15, 0, 0, ZoneOffset.ofHours(2));

	private static FinancialAssistanceEntity submittedErrand() {
		return FinancialAssistanceEntity.create().withCreated(SUBMITTED_AT);
	}

	/** An errand whose step has been to Lifecare before: the marker is set. */
	private static FinancialAssistanceEntity submittedErrandThatWasAttemptedBefore() {
		return submittedErrand().withActualisationRequestedAt(SUBMITTED_AT.plusMinutes(5));
	}

	/** By default the marker is not there and this attempt is the first to set it. */
	@BeforeEach
	void setUp() {
		lenient().when(financialAssistanceRepositoryMock.markActualisationRequestedIfAbsent(eq(ERRAND_ID), any(OffsetDateTime.class))).thenReturn(1);
	}

	@Test
	void createActualisationResolvesPartyDelegatesAndMaps() {
		when(actualisationServiceMock.createActualisation(MUNICIPALITY_ID, APPLICANT_PARTY_ID, LocalDate.of(2026, JUNE, 1), false)).thenReturn(new ActualisationResult(5012, "anna01ker", null));

		final var request = ActualisationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID)
			.withApplicationMonth("2026-06");

		final var response = service.createActualisation(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.getActualisationId()).isEqualTo(5012);
		verify(actualisationServiceMock).createActualisation(MUNICIPALITY_ID, APPLICANT_PARTY_ID, LocalDate.of(2026, JUNE, 1), false);
		// No errandId on the request → nothing recorded on an errand and no assignment.
		verify(decisionServiceMock, never()).create(any(), any(), any(), any());
		verify(errandServiceMock, never()).updateErrand(any(), any(), any(), any());
	}

	@Test
	void createActualisationForNyansokanUsesTheNewApplicationType() {
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(submittedErrand().withApplicationType("NEW")));
		when(actualisationServiceMock.createActualisation(MUNICIPALITY_ID, APPLICANT_PARTY_ID, LocalDate.of(2026, JUNE, 17), true)).thenReturn(new ActualisationResult(5012, null, null));

		final var request = ActualisationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID)
			.withApplicationMonth("2026-06")
			.withErrandId(ERRAND_ID);

		final var response = service.createActualisation(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.getActualisationId()).isEqualTo(5012);
		verify(actualisationServiceMock).createActualisation(MUNICIPALITY_ID, APPLICANT_PARTY_ID, LocalDate.of(2026, JUNE, 17), true);
	}

	@Test
	void createActualisationWithErrandIdRecordsActualisationDecisionAndAssignsCaseworker() {
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(submittedErrand()));
		when(actualisationServiceMock.createActualisation(MUNICIPALITY_ID, APPLICANT_PARTY_ID, LocalDate.of(2026, JUNE, 17), false)).thenReturn(new ActualisationResult(5012, "anna01ker", null));

		final var request = ActualisationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID)
			.withApplicationMonth("2026-06")
			.withErrandId(ERRAND_ID);

		final var response = service.createActualisation(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.getActualisationId()).isEqualTo(5012);
		// Ansökningsdatum = datum för inskickandet (errandets created), inte månadens första dag.
		verify(actualisationServiceMock).createActualisation(MUNICIPALITY_ID, APPLICANT_PARTY_ID, LocalDate.of(2026, JUNE, 17), false);

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
	void createActualisationKeepsTheLinkedInsatsOnTheErrand() {
		final var errand = submittedErrand();
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(errand));
		when(actualisationServiceMock.createActualisation(any(), any(), any(), anyBoolean())).thenReturn(new ActualisationResult(5012, null, 7700));

		service.createActualisation(MUNICIPALITY_ID, NAMESPACE, ActualisationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID)
			.withApplicationMonth("2026-06")
			.withErrandId(ERRAND_ID));

		// A targeted update: the actualisation's insats is authoritative, and no stale entity is written back.
		verify(financialAssistanceRepositoryMock).updateLifecareServiceId(ERRAND_ID, 7700);
		verify(financialAssistanceRepositoryMock, never()).save(errand);
	}

	@Test
	void createActualisationWithoutLinkedInsatsKeepsNothing() {
		final var errand = submittedErrand();
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(errand));
		when(actualisationServiceMock.createActualisation(any(), any(), any(), anyBoolean())).thenReturn(new ActualisationResult(5012, null, null));

		service.createActualisation(MUNICIPALITY_ID, NAMESPACE, ActualisationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID)
			.withApplicationMonth("2026-06")
			.withErrandId(ERRAND_ID));

		verify(financialAssistanceRepositoryMock, never()).save(any());
		assertThat(errand.getLifecareServiceId()).isNull();
	}

	/**
	 * The half of the process step that its name promised and the code never did: "Aktualisera &amp; arkivera
	 * ansökan" created the actualisation and stopped there.
	 */
	@Test
	void createActualisationArchivesTheApplicationOntoTheActualisation() {
		final var application = new SourceFile("EB-26060001.pdf", "application/pdf", "application-pdf".getBytes(UTF_8));
		final var merged = new SourceFile("sammanstallning.pdf", "application/pdf", "combined-pdf".getBytes(UTF_8));
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(submittedErrand()));
		when(actualisationServiceMock.createActualisation(any(), any(), any(), anyBoolean())).thenReturn(new ActualisationResult(5012, null, null));
		when(attachmentServiceMock.readApplicationArchiveDocuments(ERRAND_ID)).thenReturn(List.of(application, merged));
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withErrandNumber("EB-26060001"));

		service.createActualisation(MUNICIPALITY_ID, NAMESPACE, ActualisationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID));

		// Both documents, under names a caseworker can tell apart among the person's other Lifecare documents.
		verify(actualisationServiceMock).uploadAttachment(MUNICIPALITY_ID, 5012,
			new AttachmentUpload("1", "1", "Ansökan ekonomiskt bistånd EB-26060001", "Draken", "EB-26060001_ansokan.pdf", application.content()));
		verify(actualisationServiceMock).uploadAttachment(MUNICIPALITY_ID, 5012,
			new AttachmentUpload("1", "1", "Bilagor till ansökan EB-26060001", "Draken", "EB-26060001_bilagor.pdf", merged.content()));

		final var decisionCaptor = ArgumentCaptor.forClass(Decision.class);
		verify(decisionServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), decisionCaptor.capture());
		assertThat(decisionCaptor.getValue().getDescription())
			.contains("EB-26060001_ansokan.pdf")
			.contains("EB-26060001_bilagor.pdf");
	}

	/** An application can arrive with no uploaded files at all; that is not a failure, and the row says so. */
	@Test
	void createActualisationWithNoApplicationDocumentsUploadsNothingAndSaysSo() {
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(submittedErrand()));
		when(actualisationServiceMock.createActualisation(any(), any(), any(), anyBoolean())).thenReturn(new ActualisationResult(5012, null, null));
		when(attachmentServiceMock.readApplicationArchiveDocuments(ERRAND_ID)).thenReturn(List.of());

		service.createActualisation(MUNICIPALITY_ID, NAMESPACE, ActualisationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID));

		verify(actualisationServiceMock, never()).uploadAttachment(any(), any(), any());

		final var decisionCaptor = ArgumentCaptor.forClass(Decision.class);
		verify(decisionServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), decisionCaptor.capture());
		assertThat(decisionCaptor.getValue().getDescription()).contains("No application documents to archive");
	}

	/**
	 * The intake must survive a failed archive. Propagating would make the external task retry the whole step and
	 * create a second actualisation for the same application — so the failure is recorded on the errand instead.
	 */
	@Test
	void createActualisationSurvivesAFailedArchiveAndRecordsItOnTheErrand() {
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(submittedErrand()));
		when(actualisationServiceMock.createActualisation(any(), any(), any(), anyBoolean())).thenReturn(new ActualisationResult(5012, null, null));
		when(attachmentServiceMock.readApplicationArchiveDocuments(ERRAND_ID))
			.thenReturn(List.of(new SourceFile("EB-26060001.pdf", "application/pdf", "pdf".getBytes(UTF_8))));
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withErrandNumber("EB-26060001"));
		doThrow(Problem.valueOf(BAD_GATEWAY, "Lifecare refused the upload"))
			.when(actualisationServiceMock).uploadAttachment(any(), any(), any());

		final var response = service.createActualisation(MUNICIPALITY_ID, NAMESPACE, ActualisationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID));

		assertThat(response.getActualisationId()).isEqualTo(5012);

		final var decisionCaptor = ArgumentCaptor.forClass(Decision.class);
		verify(decisionServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), decisionCaptor.capture());
		assertThat(decisionCaptor.getValue().getDescription())
			.contains("id 5012")
			.contains("FAILED")
			// The exception type only: Lifecare's message may carry the file name or the applicant's details.
			.contains("ThrowableProblem")
			.doesNotContain("Lifecare refused the upload");
	}

	@Test
	void createActualisationWithErrandIdButNoResolvedCaseworkerRecordsDecisionWithoutAssigning() {
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(submittedErrand()));
		when(actualisationServiceMock.createActualisation(MUNICIPALITY_ID, APPLICANT_PARTY_ID, LocalDate.of(2026, JUNE, 17), false)).thenReturn(new ActualisationResult(5012, null, null));

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
	void createActualisationWithErrandIdButNoStoredErrandFallsBackToFirstOfApplicationMonth() {
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.empty());
		when(actualisationServiceMock.createActualisation(MUNICIPALITY_ID, APPLICANT_PARTY_ID, LocalDate.of(2026, JUNE, 1), false)).thenReturn(new ActualisationResult(5012, null, null));

		final var request = ActualisationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID)
			.withApplicationMonth("2026-06")
			.withErrandId(ERRAND_ID);

		service.createActualisation(MUNICIPALITY_ID, NAMESPACE, request);

		// No errand row to read a submission date from — the month's first day is the documented fallback.
		verify(actualisationServiceMock).createActualisation(MUNICIPALITY_ID, APPLICANT_PARTY_ID, LocalDate.of(2026, JUNE, 1), false);
	}

	@Test
	void createActualisationWithErrandIdButNoCreatedStampFallsBackToFirstOfApplicationMonth() {
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create()));
		when(actualisationServiceMock.createActualisation(MUNICIPALITY_ID, APPLICANT_PARTY_ID, LocalDate.of(2026, JUNE, 1), false)).thenReturn(new ActualisationResult(5012, null, null));

		final var request = ActualisationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID)
			.withApplicationMonth("2026-06")
			.withErrandId(ERRAND_ID);

		service.createActualisation(MUNICIPALITY_ID, NAMESPACE, request);

		verify(actualisationServiceMock).createActualisation(MUNICIPALITY_ID, APPLICANT_PARTY_ID, LocalDate.of(2026, JUNE, 1), false);
	}

	// ---- a retry never creates a second actualisation -------------------------------------------------------------

	private static ActualisationRequest requestForTheErrand() {
		return ActualisationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID)
			.withApplicationMonth("2026-06")
			.withErrandId(ERRAND_ID);
	}

	private static Decision recordedActualisation(final String value) {
		return Decision.create().withDecisionType("ACTUALISATION").withValue(value);
	}

	@Test
	void theMarkerIsCommittedBeforeLifecareIsCalledAndBeforeAnythingIsRecorded() {
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(submittedErrand()));
		when(actualisationServiceMock.createActualisation(MUNICIPALITY_ID, APPLICANT_PARTY_ID, LocalDate.of(2026, JUNE, 17), false)).thenReturn(new ActualisationResult(5012, null, null));

		final var response = service.createActualisation(MUNICIPALITY_ID, NAMESPACE, requestForTheErrand());

		assertThat(response.getActualisationId()).isEqualTo(5012);
		// The marker goes through the repository's own-transaction update, never through a save of the loaded entity
		// (which would only be committed with the intake), and it is written before Lifecare is called.
		final InOrder order = inOrder(financialAssistanceRepositoryMock, actualisationServiceMock, decisionServiceMock);
		order.verify(financialAssistanceRepositoryMock).markActualisationRequestedIfAbsent(eq(ERRAND_ID), any(OffsetDateTime.class));
		order.verify(actualisationServiceMock).createActualisation(MUNICIPALITY_ID, APPLICANT_PARTY_ID, LocalDate.of(2026, JUNE, 17), false);
		order.verify(decisionServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(Decision.class));
		verify(financialAssistanceRepositoryMock, never()).save(any());
		// The first attempt has nothing to look for.
		verify(actualisationServiceMock, never()).createOrAdoptActualisation(any(), any(), any(), anyBoolean(), any());
	}

	@Test
	void aFailureAfterLifecareCreatedTheActualisationLeavesTheMarkerBehind() {
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(submittedErrand()));
		when(actualisationServiceMock.createActualisation(any(), any(), any(), anyBoolean())).thenReturn(new ActualisationResult(5012, null, null));
		doThrow(new IllegalStateException("the database went away")).when(decisionServiceMock).create(any(), any(), any(), any());
		final var request = requestForTheErrand();

		assertThatThrownBy(() -> service.createActualisation(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(IllegalStateException.class);

		// Committed before the failure, in its own transaction: rolling the intake back does not take it along.
		final InOrder order = inOrder(financialAssistanceRepositoryMock, actualisationServiceMock);
		order.verify(financialAssistanceRepositoryMock).markActualisationRequestedIfAbsent(eq(ERRAND_ID), any(OffsetDateTime.class));
		order.verify(actualisationServiceMock).createActualisation(any(), any(), any(), anyBoolean());
		verify(financialAssistanceRepositoryMock, never()).save(any());
	}

	@Test
	void anActualisationAlreadyRecordedOnTheErrandIsReturnedAndNothingIsCreated() {
		when(decisionServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(recordedActualisation("5012")));

		final var response = service.createActualisation(MUNICIPALITY_ID, NAMESPACE, requestForTheErrand());

		assertThat(response.getActualisationId()).isEqualTo(5012);
		verifyNoInteractions(actualisationServiceMock, attachmentServiceMock, errandServiceMock);
		verify(decisionServiceMock, never()).create(any(), any(), any(), any());
		verify(financialAssistanceRepositoryMock, never()).markActualisationRequestedIfAbsent(any(), any());
		verify(financialAssistanceRepositoryMock, never()).updateLifecareServiceId(any(), any());
	}

	@Test
	void theMostRecentRecordedActualisationWins() {
		// Newest first, as the decisions are read.
		when(decisionServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(
			Decision.create().withDecisionType("RECOMMENDATION").withValue("APPROVE"),
			recordedActualisation("5013"),
			recordedActualisation("5012")));

		assertThat(service.createActualisation(MUNICIPALITY_ID, NAMESPACE, requestForTheErrand()).getActualisationId()).isEqualTo(5013);
		verifyNoInteractions(actualisationServiceMock);
	}

	@Test
	void aRecordedActualisationWithoutAnIdIsNotAnActualisation() {
		when(decisionServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(recordedActualisation(null), recordedActualisation(" "), recordedActualisation("not-a-number")));
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(submittedErrand()));
		when(actualisationServiceMock.createActualisation(any(), any(), any(), anyBoolean())).thenReturn(new ActualisationResult(5012, null, null));

		final var response = service.createActualisation(MUNICIPALITY_ID, NAMESPACE, requestForTheErrand());

		assertThat(response.getActualisationId()).isEqualTo(5012);
		verify(actualisationServiceMock).createActualisation(any(), any(), any(), anyBoolean());
	}

	@Test
	void aMarkerWithNoRecordedIdLooksForTheEarlierAttemptAndAdoptsWhatItFinds() {
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(submittedErrandThatWasAttemptedBefore()));
		when(actualisationServiceMock.createOrAdoptActualisation(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), eq(LocalDate.of(2026, JUNE, 17)), eq(false), any()))
			.thenReturn(new ActualisationResult(5012, "anna01ker", 7700, true));
		when(attachmentServiceMock.readApplicationArchiveDocuments(ERRAND_ID)).thenReturn(List.of());

		final var response = service.createActualisation(MUNICIPALITY_ID, NAMESPACE, requestForTheErrand());

		assertThat(response.getActualisationId()).isEqualTo(5012);
		// Nothing is created, and the marker stays as the first attempt left it.
		verify(actualisationServiceMock, never()).createActualisation(any(), any(), any(), anyBoolean());
		verify(financialAssistanceRepositoryMock, never()).markActualisationRequestedIfAbsent(any(), any());

		// What was found is recorded like what would have been created, and the audit trail says which it was.
		final var decisionCaptor = ArgumentCaptor.forClass(Decision.class);
		verify(decisionServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), decisionCaptor.capture());
		assertThat(decisionCaptor.getValue().getValue()).isEqualTo("5012");
		assertThat(decisionCaptor.getValue().getDescription())
			.contains("found in Lifecare (id 5012)")
			.contains("no second one was made")
			.doesNotContain("created in Lifecare");
		verify(financialAssistanceRepositoryMock).updateLifecareServiceId(ERRAND_ID, 7700);
		verify(errandServiceMock).updateErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(PatchErrand.class));
	}

	@Test
	void aMarkerSetWhileThisAttemptWasStartingCountsAsAnEarlierAttempt() {
		// The read saw no marker, but another attempt set it before ours: the conditional update reports 0.
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(submittedErrand()));
		when(financialAssistanceRepositoryMock.markActualisationRequestedIfAbsent(eq(ERRAND_ID), any(OffsetDateTime.class))).thenReturn(0);
		when(actualisationServiceMock.createOrAdoptActualisation(any(), any(), any(), anyBoolean(), any())).thenReturn(new ActualisationResult(5012, null, null, true));

		service.createActualisation(MUNICIPALITY_ID, NAMESPACE, requestForTheErrand());

		verify(actualisationServiceMock).createOrAdoptActualisation(any(), any(), any(), anyBoolean(), any());
		verify(actualisationServiceMock, never()).createActualisation(any(), any(), any(), anyBoolean());
	}

	@Test
	void anActualisationRecordedOnAnotherErrandIsReportedAsClaimed() {
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(submittedErrandThatWasAttemptedBefore()));
		when(actualisationServiceMock.createOrAdoptActualisation(any(), any(), any(), anyBoolean(), any())).thenReturn(new ActualisationResult(5012, null, null, true));
		when(decisionServiceMock.existsOnAnotherErrand("ACTUALISATION", "5011", ERRAND_ID)).thenReturn(true);
		when(decisionServiceMock.existsOnAnotherErrand("ACTUALISATION", "5012", ERRAND_ID)).thenReturn(false);

		service.createActualisation(MUNICIPALITY_ID, NAMESPACE, requestForTheErrand());

		final ArgumentCaptor<IntPredicate> claimed = ArgumentCaptor.forClass(IntPredicate.class);
		verify(actualisationServiceMock).createOrAdoptActualisation(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), eq(false), claimed.capture());
		assertThat(claimed.getValue().test(5011)).isTrue();
		assertThat(claimed.getValue().test(5012)).isFalse();
	}

	@Test
	void aNyansokanThatWasAttemptedBeforeLooksForTheNyansokanType() {
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(submittedErrandThatWasAttemptedBefore().withApplicationType("NEW")));
		when(actualisationServiceMock.createOrAdoptActualisation(any(), any(), any(), eq(true), any())).thenReturn(new ActualisationResult(5012, null, null, true));

		service.createActualisation(MUNICIPALITY_ID, NAMESPACE, requestForTheErrand());

		verify(actualisationServiceMock).createOrAdoptActualisation(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), eq(LocalDate.of(2026, JUNE, 17)), eq(true), any());
	}

	@Test
	void whenTheEarlierAttemptCannotBeToldApartNothingIsCreatedOrRecorded() {
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(submittedErrandThatWasAttemptedBefore()));
		when(actualisationServiceMock.createOrAdoptActualisation(any(), any(), any(), anyBoolean(), any()))
			.thenThrow(Problem.valueOf(CONFLICT, "Lifecare holds 2 actualisations that an earlier attempt of this step may have created"));

		assertThatThrownBy(() -> service.createActualisation(MUNICIPALITY_ID, NAMESPACE, requestForTheErrand()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", CONFLICT);

		verify(actualisationServiceMock, never()).createActualisation(any(), any(), any(), anyBoolean());
		verify(decisionServiceMock, never()).create(any(), any(), any(), any());
		verifyNoInteractions(attachmentServiceMock, errandServiceMock);
		verify(financialAssistanceRepositoryMock, never()).updateLifecareServiceId(any(), any());
	}

	@Test
	void whenTheLookupForTheEarlierAttemptFailsNothingIsCreatedOrRecorded() {
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(submittedErrandThatWasAttemptedBefore()));
		when(actualisationServiceMock.createOrAdoptActualisation(any(), any(), any(), anyBoolean(), any()))
			.thenThrow(Problem.valueOf(BAD_GATEWAY, "Error fetching actualisations in Lifecare FamilyCare: 503"));

		assertThatThrownBy(() -> service.createActualisation(MUNICIPALITY_ID, NAMESPACE, requestForTheErrand()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY);

		verify(actualisationServiceMock, never()).createActualisation(any(), any(), any(), anyBoolean());
		verify(decisionServiceMock, never()).create(any(), any(), any(), any());
	}

	@Test
	void anErrandWithNoStoredApplicationHasNothingToHoldTheMarkerAndCreatesAsBefore() {
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.empty());
		when(actualisationServiceMock.createActualisation(any(), any(), any(), anyBoolean())).thenReturn(new ActualisationResult(5012, null, null));

		service.createActualisation(MUNICIPALITY_ID, NAMESPACE, requestForTheErrand());

		verify(financialAssistanceRepositoryMock, never()).markActualisationRequestedIfAbsent(any(), any());
		verify(actualisationServiceMock).createActualisation(any(), any(), any(), anyBoolean());
		verify(decisionServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(Decision.class));
	}

	@Test
	void aCallWithoutAnErrandIsNeitherLookedUpNorMarked() {
		when(actualisationServiceMock.createActualisation(any(), any(), any(), anyBoolean())).thenReturn(new ActualisationResult(5012, null, null));

		service.createActualisation(MUNICIPALITY_ID, NAMESPACE, ActualisationRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06"));

		verifyNoInteractions(decisionServiceMock, financialAssistanceRepositoryMock);
		verify(actualisationServiceMock).createActualisation(any(), any(), any(), anyBoolean());
	}

	@Test
	void listActualisationsResolvesPartyDefaultsPeriodAndMaps() {
		final var summary = new ActualisationSummary(5012, "Ansökan", "Ekonomiskt bistånd", "2026-06-01", "Nyansökan", "Försörjningsstöd",
			"Den enskilde", "Anna Andersson", "IFO", "Pågående", 8801, 7700, 9900);
		when(actualisationServiceMock.listActualisations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of(summary));

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
		verify(actualisationServiceMock).listActualisations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), fromCaptor.capture(), toCaptor.capture());
		assertThat(toCaptor.getValue()).isEqualTo(LocalDate.now());
		assertThat(fromCaptor.getValue()).isEqualTo(toCaptor.getValue().minusMonths(24));
	}

	@Test
	void listActualisationsUsesExplicitPeriod() {
		when(actualisationServiceMock.listActualisations(MUNICIPALITY_ID, APPLICANT_PARTY_ID, LocalDate.of(2026, JANUARY, 1), LocalDate.of(2026, JUNE, 30))).thenReturn(List.of());

		final var result = service.listActualisations(MUNICIPALITY_ID, APPLICANT_PARTY_ID, LocalDate.of(2026, JANUARY, 1), LocalDate.of(2026, JUNE, 30));

		assertThat(result).isEmpty();
		verify(actualisationServiceMock).listActualisations(MUNICIPALITY_ID, APPLICANT_PARTY_ID, LocalDate.of(2026, JANUARY, 1), LocalDate.of(2026, JUNE, 30));
	}

	private void applicantOwnsActualisation5012() {
		when(actualisationServiceMock.listActualisations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(LocalDate.class), any(LocalDate.class)))
			.thenReturn(List.of(new ActualisationSummary(5012, "Ansökan", "EB", "2026-06-01", "Nyansökan", "Försörjningsstöd", "Den enskilde", "Anna", "IFO", "Pågående", 8801, 7700, 9900)));
	}

	@Test
	void archiveToActualisationForwardsFileWithDefaultsWhenNoMetadata() {
		applicantOwnsActualisation5012();
		final var file = new MockMultipartFile("file", "tillaggsansokan.pdf", "application/pdf", new byte[] {
			1, 2, 3
		});

		service.archiveToActualisation(MUNICIPALITY_ID, NAMESPACE, APPLICANT_PARTY_ID, 5012, file, null);

		verify(actualisationServiceMock).uploadAttachment(MUNICIPALITY_ID, 5012, new AttachmentUpload("1", "1", "tillaggsansokan.pdf", "Draken", "tillaggsansokan.pdf", new byte[] {
			1, 2, 3
		}));
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

		verify(actualisationServiceMock).uploadAttachment(MUNICIPALITY_ID, 5012, new AttachmentUpload("KOMPLETTERING", "MYNDIGHET", "Tilläggsansökan juni", "Sundsvalls kommun", "tillaggsansokan.pdf", new byte[] {
			9
		}));
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

		verify(actualisationServiceMock).uploadAttachment(eq(MUNICIPALITY_ID), eq(5012),
			argThat(attachment -> "tillaggsansokan.pdf".equals(attachment.fileName()) && "1".equals(attachment.documentType()) && "1".equals(attachment.documentSenderType())
				&& "tillaggsansokan.pdf".equals(attachment.title()) && "Draken".equals(attachment.senderName())));

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
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST)
			.hasMessage("Bad Request: Could not read the uploaded file: stream closed");

		verify(actualisationServiceMock, never()).uploadAttachment(eq(MUNICIPALITY_ID), any(), any());
	}

	@Test
	void archiveToActualisationForeignActualisationYields404() {
		applicantOwnsActualisation5012();
		final var file = new MockMultipartFile("file", "x.pdf", "application/pdf", new byte[] {
			1
		});

		assertThatThrownBy(() -> service.archiveToActualisation(MUNICIPALITY_ID, NAMESPACE, APPLICANT_PARTY_ID, 9999, file, null))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: No Lifecare actualisation '9999' found for the given applicant");

		verify(actualisationServiceMock, never()).uploadAttachment(eq(MUNICIPALITY_ID), any(), any());
	}
}
