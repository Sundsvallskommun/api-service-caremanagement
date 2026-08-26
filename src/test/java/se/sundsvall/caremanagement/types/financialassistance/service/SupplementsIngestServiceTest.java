package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.document.service.DocumentService;
import se.sundsvall.caremanagement.document.service.LifecareDocumentMirror;
import se.sundsvall.caremanagement.journal.service.JournalEntryService;
import se.sundsvall.caremanagement.journal.service.LifecareJournalEntryMirror;
import se.sundsvall.caremanagement.shared.MirrorOutcome;
import se.sundsvall.caremanagement.types.financialassistance.api.model.JobStimulusPeriod;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareDocumentRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareJobStimulus;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareJobStimulusParty;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareJobStimulusPeriod;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareReminder;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareSupplements;
import se.sundsvall.caremanagement.types.financialassistance.api.model.MonitoringRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SupplementsIngestOutcome;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaMonitoringRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaMonitoringEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplementsIngestServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private MonitoringService monitoringServiceMock;

	@Mock
	private FaMonitoringRepository monitoringRepositoryMock;

	@Mock
	private JournalEntryService journalEntryServiceMock;

	@Mock
	private DocumentService documentServiceMock;

	@Mock
	private JobStimulusPeriodService jobStimulusPeriodServiceMock;

	@InjectMocks
	private SupplementsIngestService service;

	private static LifecareReminder reminder(final String reminderId, final String reminderDate) {
		return new LifecareReminder(reminderId, reminderDate, "1", "Pågår", "2", "Normal", "3", "Manuell bevakning insats",
			"Fritext från handläggaren", "TEST", "Test Handläggare", "7083", "IFO.Insats");
	}

	private static LifecareDocumentRow documentRow(final String id, final String documentType) {
		return new LifecareDocumentRow(id, "Journalanteckning", "2026-08-05", "11:06", "Journalanteckning", "1", documentType,
			"<p>Hej! Vill bara informera att jag f&aring;tt jobb.</p>", "RPA_031DEV", "2026-08-05", "JournalNote");
	}

	@Test
	void emptyEnvelopeTouchesNothing() {
		final var result = service.ingest(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new LifecareSupplements("2026-08-25", null, null, null));

		assertThat(result.results()).isEmpty();
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verifyNoInteractions(monitoringServiceMock, journalEntryServiceMock, documentServiceMock, jobStimulusPeriodServiceMock);
	}

	@Test
	void newReminderIsCreatedAsLifecareMonitoring() {
		when(monitoringRepositoryMock.findByErrandIdAndLifecareId(ERRAND_ID, "4")).thenReturn(Optional.empty());

		final var result = service.ingest(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			new LifecareSupplements(null, List.of(reminder("4", "2026-10-17")), null, null));

		assertThat(result.results()).containsExactly(new SupplementsIngestOutcome("reminders", "4", "CREATED", null));

		final ArgumentCaptor<MonitoringRequest> captor = ArgumentCaptor.forClass(MonitoringRequest.class);
		verify(monitoringServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), captor.capture());
		final var request = captor.getValue();
		assertThat(request.getSource()).isEqualTo("LIFECARE");
		assertThat(request.getLifecareId()).isEqualTo("4");
		assertThat(request.getTitle()).isEqualTo("Manuell bevakning insats");
		assertThat(request.getDescription()).isEqualTo("Fritext från handläggaren");
		assertThat(request.getStartDate()).isEqualTo(LocalDate.parse("2026-10-17"));
		assertThat(request.getCreatedBy()).isEqualTo("TEST");
	}

	@Test
	void redeliveredReminderReportsUpdated() {
		when(monitoringRepositoryMock.findByErrandIdAndLifecareId(ERRAND_ID, "4")).thenReturn(Optional.of(FaMonitoringEntity.create()));

		final var result = service.ingest(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			new LifecareSupplements(null, List.of(reminder("4", "2026-10-17")), null, null));

		assertThat(result.results()).containsExactly(new SupplementsIngestOutcome("reminders", "4", "UPDATED", null));
		verify(monitoringServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(MonitoringRequest.class));
	}

	@Test
	void reminderWithoutIdIsSkipped() {
		final var result = service.ingest(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			new LifecareSupplements(null, List.of(reminder("", "2026-10-17")), null, null));

		assertThat(result.results()).hasSize(1);
		assertThat(result.results().getFirst().outcome()).isEqualTo("SKIPPED");
		verifyNoInteractions(monitoringServiceMock);
	}

	@Test
	void reminderWithUnparseableDateFailsWithoutBreakingTheBatch() {
		when(monitoringRepositoryMock.findByErrandIdAndLifecareId(ERRAND_ID, "5")).thenReturn(Optional.empty());

		final var result = service.ingest(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			new LifecareSupplements(null, List.of(reminder("4", ""), reminder("5", "2026-10-17")), null, null));

		assertThat(result.results()).hasSize(2);
		assertThat(result.results().getFirst().outcome()).isEqualTo("FAILED");
		assertThat(result.results().getFirst().detail()).contains("reminderDate");
		assertThat(result.results().getLast().outcome()).isEqualTo("CREATED");
	}

	@Test
	void journalNoteRowIsMirroredIntoTheJournalModule() {
		when(journalEntryServiceMock.mirrorFromLifecare(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(LifecareJournalEntryMirror.class)))
			.thenReturn(new MirrorOutcome("je-1", true));

		final var result = service.ingest(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			new LifecareSupplements(null, null, List.of(documentRow("27", "3")), null));

		assertThat(result.results()).containsExactly(new SupplementsIngestOutcome("documents", "27", "CREATED", null));

		final ArgumentCaptor<LifecareJournalEntryMirror> captor = ArgumentCaptor.forClass(LifecareJournalEntryMirror.class);
		verify(journalEntryServiceMock).mirrorFromLifecare(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), captor.capture());
		final var mirror = captor.getValue();
		assertThat(mirror.lifecareId()).isEqualTo("27");
		assertThat(mirror.type()).isEqualTo("Journalanteckning");
		assertThat(mirror.heading()).isEqualTo("Journalanteckning");
		assertThat(mirror.text()).isEqualTo("Hej! Vill bara informera att jag fått jobb.");
		assertThat(mirror.entryDateTime().toLocalDate()).isEqualTo(LocalDate.parse("2026-08-05"));
		assertThat(mirror.entryDateTime().toLocalTime()).isEqualTo(LocalTime.of(11, 6));
		assertThat(mirror.createdBy()).isEqualTo("RPA_031DEV");
		verifyNoInteractions(documentServiceMock);
	}

	@Test
	void regularDocumentRowIsMirroredIntoTheDocumentModule() {
		when(documentServiceMock.mirrorFromLifecare(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(LifecareDocumentMirror.class)))
			.thenReturn(new MirrorOutcome("doc-1", false));

		final var result = service.ingest(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			new LifecareSupplements(null, null, List.of(documentRow("28", "0")), null));

		assertThat(result.results()).containsExactly(new SupplementsIngestOutcome("documents", "28", "UPDATED", null));
		verifyNoInteractions(journalEntryServiceMock);
	}

	@Test
	void unroutableDocumentRowIsSkipped() {
		final var result = service.ingest(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			new LifecareSupplements(null, null, List.of(documentRow("29", "7")), null));

		assertThat(result.results()).hasSize(1);
		assertThat(result.results().getFirst().outcome()).isEqualTo("SKIPPED");
		assertThat(result.results().getFirst().detail()).contains("unroutable");
		verifyNoInteractions(journalEntryServiceMock, documentServiceMock);
	}

	@Test
	void documentRowWithoutIdIsSkipped() {
		final var result = service.ingest(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			new LifecareSupplements(null, null, List.of(documentRow(null, "3")), null));

		assertThat(result.results()).hasSize(1);
		assertThat(result.results().getFirst().outcome()).isEqualTo("SKIPPED");
	}

	@Test
	void mirrorFailureIsReportedWithoutBreakingTheBatch() {
		when(journalEntryServiceMock.mirrorFromLifecare(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(LifecareJournalEntryMirror.class)))
			.thenThrow(new IllegalStateException("boom"));
		when(documentServiceMock.mirrorFromLifecare(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(LifecareDocumentMirror.class)))
			.thenReturn(new MirrorOutcome("doc-1", true));

		final var result = service.ingest(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			new LifecareSupplements(null, null, List.of(documentRow("27", "3"), documentRow("28", "0")), null));

		assertThat(result.results()).hasSize(2);
		assertThat(result.results().getFirst().outcome()).isEqualTo("FAILED");
		assertThat(result.results().getFirst().detail()).contains("boom");
		assertThat(result.results().getLast().outcome()).isEqualTo("CREATED");
	}

	@Test
	void jobStimulusReplacesTheFullPeriodSet() {
		when(jobStimulusPeriodServiceMock.replaceAll(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), anyList())).thenReturn(2);

		final var jobStimulus = new LifecareJobStimulus(
			new LifecareJobStimulusParty(List.of(
				new LifecareJobStimulusPeriod("2021-01-01", "2021-12-31", false),
				new LifecareJobStimulusPeriod("2020-01-01", "2020-06-30", true))),
			new LifecareJobStimulusParty(List.of(
				new LifecareJobStimulusPeriod("2022-01-01", "", null))));

		final var result = service.ingest(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			new LifecareSupplements(null, null, null, jobStimulus));

		assertThat(result.results()).containsExactly(new SupplementsIngestOutcome("jobStimulus", null, "REPLACED", "2 period(s)"));

		final ArgumentCaptor<List<JobStimulusPeriod>> captor = ArgumentCaptor.captor();
		verify(jobStimulusPeriodServiceMock).replaceAll(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), captor.capture());
		assertThat(captor.getValue()).containsExactly(
			new JobStimulusPeriod("APPLICANT", LocalDate.parse("2021-01-01"), LocalDate.parse("2021-12-31")),
			new JobStimulusPeriod("CO_APPLICANT", LocalDate.parse("2022-01-01"), null));
	}

	@Test
	void jobStimulusPeriodWithoutFromDateFailsButTheRestReplace() {
		when(jobStimulusPeriodServiceMock.replaceAll(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), anyList())).thenReturn(1);

		final var jobStimulus = new LifecareJobStimulus(
			new LifecareJobStimulusParty(List.of(
				new LifecareJobStimulusPeriod("", "2021-12-31", false),
				new LifecareJobStimulusPeriod("2021-01-01", "2021-12-31", false))),
			null);

		final var result = service.ingest(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			new LifecareSupplements(null, null, null, jobStimulus));

		assertThat(result.results()).hasSize(2);
		assertThat(result.results().getFirst().outcome()).isEqualTo("FAILED");
		assertThat(result.results().getFirst().detail()).contains("APPLICANT");
		assertThat(result.results().getLast().outcome()).isEqualTo("REPLACED");
	}

	@Test
	void emptyJobStimulusSectionEmptiesThePeriodSet() {
		when(jobStimulusPeriodServiceMock.replaceAll(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), anyList())).thenReturn(0);

		final var result = service.ingest(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			new LifecareSupplements(null, null, null, new LifecareJobStimulus(new LifecareJobStimulusParty(List.of()), null)));

		assertThat(result.results()).containsExactly(new SupplementsIngestOutcome("jobStimulus", null, "REPLACED", "0 period(s)"));
		verify(jobStimulusPeriodServiceMock).replaceAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, List.of());
	}
}
