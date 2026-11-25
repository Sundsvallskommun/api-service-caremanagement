package se.sundsvall.caremanagement.types.financialassistance.service.event;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.service.event.ErrandCreated;
import se.sundsvall.caremanagement.core.spi.ErrandQueryService;
import se.sundsvall.caremanagement.operaton.service.ProcessService;
import se.sundsvall.caremanagement.types.financialassistance.service.event.FinancialAssistanceErrandCreatedProcessor.Outcome;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUGS;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_NEW;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_RENEWAL;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_SUPPLEMENTARY;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_RECEIVED;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_UNDER_REVIEW;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_WITHDRAWN;
import static se.sundsvall.caremanagement.types.financialassistance.service.event.FinancialAssistanceProcessStarter.PROCESS_DEFINITION_NAME;
import static se.sundsvall.caremanagement.types.financialassistance.service.event.FinancialAssistanceProcessStarter.PROCESS_DEFINITION_NAME_NEW;
import static se.sundsvall.caremanagement.types.financialassistance.service.event.FinancialAssistanceProcessStarter.PROCESS_DEFINITION_NAME_SUPPLEMENTARY;

@ExtendWith(MockitoExtension.class)
class FinancialAssistanceProcessStartRetryWorkerTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final Duration MIN_AGE = Duration.ofMinutes(10);
	private static final Duration MAX_AGE = Duration.ofDays(7);

	@Mock
	private ErrandQueryService errandQueryServiceMock;

	@Mock
	private ProcessService processServiceMock;

	@Mock
	private FinancialAssistanceErrandCreatedProcessor processorMock;

	private FinancialAssistanceProcessStartRetryWorker worker;

	@BeforeEach
	void setUp() {
		worker = worker(true);
	}

	private FinancialAssistanceProcessStartRetryWorker worker(final boolean enabled) {
		return new FinancialAssistanceProcessStartRetryWorker(errandQueryServiceMock, processServiceMock, processorMock,
			new FinancialAssistanceProcessStartRetryProperties(enabled, MUNICIPALITY_ID, NAMESPACE, MIN_AGE, MAX_AGE));
	}

	private static Errand errand(final String id, final String typeSlug, final Duration age) {
		return Errand.create()
			.withId(id)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withTypeSlug(typeSlug)
			.withStatus(STATUS_RECEIVED)
			.withReporterUserId("reporter")
			.withCreated(OffsetDateTime.now().minus(age));
	}

	private void whenSelected(final Errand... errands) {
		when(errandQueryServiceMock.findWithoutProcessInstance(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(), eq(STATUS_RECEIVED), any(), any()))
			.thenReturn(List.of(errands));
	}

	/** The errand as a fresh read finds it: unchanged, so still worth starting. */
	private void whenStillUnstarted(final Errand errand) {
		when(errandQueryServiceMock.findErrand(MUNICIPALITY_ID, NAMESPACE, errand.getId())).thenReturn(Optional.of(errand));
	}

	/** The errand as a fresh read finds it before the start, then after it: the second read carries the linked instance. */
	private void whenUnstartedThenLinked(final Errand errand) {
		final var linked = Errand.create().withId(errand.getId()).withStatus(STATUS_UNDER_REVIEW).withProcessInstanceId("instance-" + errand.getId());
		when(errandQueryServiceMock.findErrand(MUNICIPALITY_ID, NAMESPACE, errand.getId())).thenReturn(Optional.of(errand), Optional.of(linked));
	}

	@Test
	void selectsUnstartedErrandsBetweenMaxAgeAndMinAge() {
		whenSelected();

		final var result = worker.retryUnstarted();

		final var from = ArgumentCaptor.forClass(OffsetDateTime.class);
		final var to = ArgumentCaptor.forClass(OffsetDateTime.class);
		verify(errandQueryServiceMock).findWithoutProcessInstance(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUGS), eq(STATUS_RECEIVED), from.capture(), to.capture());
		// The window reaches one day past max-age back, only so an errand that just aged out can be reported; the retrying
		// itself stops at max-age (see the aged-out tests).
		assertThat(from.getValue()).isCloseTo(OffsetDateTime.now().minus(MAX_AGE).minusHours(24), within(5, SECONDS));
		assertThat(to.getValue()).isCloseTo(OffsetDateTime.now().minus(MIN_AGE), within(5, SECONDS));
		assertThat(result).isEqualTo(new FinancialAssistanceProcessStartRetryWorker.Result(0, 0, 0, 0, 0, 0));
		verifyNoInteractions(processServiceMock, processorMock);
	}

	@Test
	void startsTheProcessOfAnErrandThatHasNoRunningInstance() {
		final var errand = errand("e1", SLUG_RENEWAL, Duration.ofHours(1)).withAssignedUserId("jane01doe");
		whenSelected(errand);
		whenUnstartedThenLinked(errand);
		when(processServiceMock.activeBusinessKeys(MUNICIPALITY_ID, PROCESS_DEFINITION_NAME)).thenReturn(Set.of("some-other-errand"));
		when(processorMock.assignAndClassify(any())).thenReturn(Outcome.PROCEED);

		final var result = worker.retryUnstarted();

		final var classified = ArgumentCaptor.forClass(ErrandCreated.class);
		final var started = ArgumentCaptor.forClass(ErrandCreated.class);
		verify(processorMock).assignAndClassify(classified.capture());
		verify(processorMock).startProcess(started.capture());
		// The same event the creation classified and started from, so the type picks the process and the variables match.
		assertThat(started.getValue()).isEqualTo(classified.getValue());
		assertThat(started.getValue().errandId()).isEqualTo("e1");
		assertThat(started.getValue().typeSlug()).isEqualTo(SLUG_RENEWAL);
		assertThat(started.getValue().municipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(started.getValue().namespace()).isEqualTo(NAMESPACE);
		assertThat(started.getValue().assignedUserId()).isEqualTo("jane01doe");
		assertThat(result).isEqualTo(new FinancialAssistanceProcessStartRetryWorker.Result(1, 1, 0, 0, 0, 0));
	}

	@Test
	void classifiesBeforeStartingSoAGuardCanStillHoldTheErrand() {
		final var errand = errand("e1", SLUG_RENEWAL, Duration.ofHours(1));
		whenSelected(errand);
		whenUnstartedThenLinked(errand);
		when(processServiceMock.activeBusinessKeys(MUNICIPALITY_ID, PROCESS_DEFINITION_NAME)).thenReturn(Set.of());
		when(processorMock.assignAndClassify(any())).thenReturn(Outcome.PROCEED);

		worker.retryUnstarted();

		final var order = inOrder(processorMock);
		order.verify(processorMock).assignAndClassify(any());
		order.verify(processorMock).startProcess(any());
	}

	@Test
	void leavesAnErrandAloneWhenTheEngineAlreadyRunsItsProcess() {
		final var errand = errand("e1", SLUG_RENEWAL, Duration.ofHours(1));
		whenSelected(errand);
		when(processServiceMock.activeBusinessKeys(MUNICIPALITY_ID, PROCESS_DEFINITION_NAME)).thenReturn(Set.of("e1"));

		final var logs = capturingLogs(() -> assertThat(worker.retryUnstarted())
			.isEqualTo(new FinancialAssistanceProcessStartRetryWorker.Result(1, 0, 1, 0, 0, 0)));

		// The start took, only the write back failed: nothing is started, classified or written, and it is not an error.
		verifyNoInteractions(processorMock);
		verify(errandQueryServiceMock, never()).findErrand(any(), any(), any());
		assertThat(logs).singleElement().satisfies(event -> {
			assertThat(event.getLevel()).isEqualTo(Level.INFO);
			assertThat(event.getFormattedMessage()).contains("e1").contains(PROCESS_DEFINITION_NAME);
		});
	}

	@Test
	void asksTheEngineOncePerProcessForTheWholeBatch() {
		final var renewalOne = errand("r1", SLUG_RENEWAL, Duration.ofHours(1));
		final var renewalTwo = errand("r2", SLUG_RENEWAL, Duration.ofHours(2));
		final var newApplication = errand("n1", SLUG_NEW, Duration.ofHours(1));
		final var supplementary = errand("s1", SLUG_SUPPLEMENTARY, Duration.ofHours(1));
		whenSelected(renewalOne, renewalTwo, newApplication, supplementary);
		when(processServiceMock.activeBusinessKeys(MUNICIPALITY_ID, PROCESS_DEFINITION_NAME)).thenReturn(Set.of("r1", "r2"));
		when(processServiceMock.activeBusinessKeys(MUNICIPALITY_ID, PROCESS_DEFINITION_NAME_NEW)).thenReturn(Set.of("n1"));
		when(processServiceMock.activeBusinessKeys(MUNICIPALITY_ID, PROCESS_DEFINITION_NAME_SUPPLEMENTARY)).thenReturn(Set.of("s1"));

		final var result = worker.retryUnstarted();

		verify(processServiceMock, times(1)).activeBusinessKeys(MUNICIPALITY_ID, PROCESS_DEFINITION_NAME);
		verify(processServiceMock, times(1)).activeBusinessKeys(MUNICIPALITY_ID, PROCESS_DEFINITION_NAME_NEW);
		verify(processServiceMock, times(1)).activeBusinessKeys(MUNICIPALITY_ID, PROCESS_DEFINITION_NAME_SUPPLEMENTARY);
		assertThat(result).isEqualTo(new FinancialAssistanceProcessStartRetryWorker.Result(4, 0, 4, 0, 0, 0));
	}

	@Test
	void runningInstancesOfAnotherErrandDoNotCount() {
		final var errand = errand("e1", SLUG_NEW, Duration.ofHours(1));
		whenSelected(errand);
		whenUnstartedThenLinked(errand);
		// Running for e1 — but of the renewal process, which is not the one a new application starts.
		when(processServiceMock.activeBusinessKeys(MUNICIPALITY_ID, PROCESS_DEFINITION_NAME_NEW)).thenReturn(Set.of());
		when(processorMock.assignAndClassify(any())).thenReturn(Outcome.PROCEED);

		final var result = worker.retryUnstarted();

		verify(processServiceMock, never()).activeBusinessKeys(MUNICIPALITY_ID, PROCESS_DEFINITION_NAME);
		assertThat(result.started()).isOne();
	}

	@Test
	void skipsAnErrandThatMovedOnSinceTheBatchWasRead() {
		final var moved = errand("e1", SLUG_RENEWAL, Duration.ofHours(1));
		final var linked = errand("e2", SLUG_RENEWAL, Duration.ofHours(1));
		whenSelected(moved, linked);
		when(processServiceMock.activeBusinessKeys(MUNICIPALITY_ID, PROCESS_DEFINITION_NAME)).thenReturn(Set.of());
		when(errandQueryServiceMock.findErrand(MUNICIPALITY_ID, NAMESPACE, "e1")).thenReturn(Optional.of(errand("e1", SLUG_RENEWAL, Duration.ofHours(1)).withStatus(STATUS_UNDER_REVIEW)));
		when(errandQueryServiceMock.findErrand(MUNICIPALITY_ID, NAMESPACE, "e2")).thenReturn(Optional.of(errand("e2", SLUG_RENEWAL, Duration.ofHours(1)).withProcessInstanceId("instance")));

		final var result = worker.retryUnstarted();

		verifyNoInteractions(processorMock);
		assertThat(result).isEqualTo(new FinancialAssistanceProcessStartRetryWorker.Result(2, 0, 0, 0, 0, 0));
	}

	@Test
	void neverStartsAProcessForAnErrandWithdrawnSinceTheBatchWasRead() {
		// Withdrawn after the selection (which only takes RECEIVED errands): a process started now would never be ended.
		final var withdrawn = errand("e1", SLUG_RENEWAL, Duration.ofHours(1));
		whenSelected(withdrawn);
		when(processServiceMock.activeBusinessKeys(MUNICIPALITY_ID, PROCESS_DEFINITION_NAME)).thenReturn(Set.of());
		when(errandQueryServiceMock.findErrand(MUNICIPALITY_ID, NAMESPACE, "e1")).thenReturn(Optional.of(errand("e1", SLUG_RENEWAL, Duration.ofHours(1)).withStatus(STATUS_WITHDRAWN)));

		final var result = worker.retryUnstarted();

		verifyNoInteractions(processorMock);
		assertThat(result).isEqualTo(new FinancialAssistanceProcessStartRetryWorker.Result(1, 0, 0, 0, 0, 0));
	}

	@Test
	void skipsAnErrandThatIsGone() {
		final var errand = errand("e1", SLUG_RENEWAL, Duration.ofHours(1));
		whenSelected(errand);
		when(processServiceMock.activeBusinessKeys(MUNICIPALITY_ID, PROCESS_DEFINITION_NAME)).thenReturn(Set.of());
		when(errandQueryServiceMock.findErrand(MUNICIPALITY_ID, NAMESPACE, "e1")).thenReturn(Optional.empty());

		worker.retryUnstarted();

		verifyNoInteractions(processorMock);
	}

	@Test
	void doesNotStartAnErrandAGuardHolds() {
		final var errand = errand("e1", SLUG_RENEWAL, Duration.ofHours(1));
		whenSelected(errand);
		whenStillUnstarted(errand);
		when(processServiceMock.activeBusinessKeys(MUNICIPALITY_ID, PROCESS_DEFINITION_NAME)).thenReturn(Set.of());
		when(processorMock.assignAndClassify(any())).thenReturn(Outcome.FROZEN);

		final var result = worker.retryUnstarted();

		verify(processorMock, never()).startProcess(any());
		assertThat(result).isEqualTo(new FinancialAssistanceProcessStartRetryWorker.Result(1, 0, 0, 1, 0, 0));
	}

	@Test
	void doesNotStartAnErrandWithoutFinancialAssistanceData() {
		final var errand = errand("e1", SLUG_RENEWAL, Duration.ofHours(1));
		whenSelected(errand);
		whenStillUnstarted(errand);
		when(processServiceMock.activeBusinessKeys(MUNICIPALITY_ID, PROCESS_DEFINITION_NAME)).thenReturn(Set.of());
		when(processorMock.assignAndClassify(any())).thenReturn(Outcome.NOT_FINANCIAL_ASSISTANCE);

		final var result = worker.retryUnstarted();

		verify(processorMock, never()).startProcess(any());
		assertThat(result).isEqualTo(new FinancialAssistanceProcessStartRetryWorker.Result(1, 0, 0, 0, 0, 0));
	}

	@Test
	void aFailingErrandDoesNotStopTheBatch() {
		final var failing = errand("e1", SLUG_RENEWAL, Duration.ofHours(2));
		final var starting = errand("e2", SLUG_RENEWAL, Duration.ofHours(1));
		whenSelected(failing, starting);
		whenStillUnstarted(failing);
		whenUnstartedThenLinked(starting);
		when(processServiceMock.activeBusinessKeys(MUNICIPALITY_ID, PROCESS_DEFINITION_NAME)).thenReturn(Set.of());
		when(processorMock.assignAndClassify(any())).thenThrow(new IllegalStateException("token endpoint said 503 for 19121212-1212")).thenReturn(Outcome.PROCEED);

		final var logs = capturingLogs(() -> assertThat(worker.retryUnstarted())
			.isEqualTo(new FinancialAssistanceProcessStartRetryWorker.Result(2, 1, 0, 0, 1, 0)));

		verify(processorMock, times(1)).startProcess(any());
		assertThat(logs).filteredOn(event -> event.getLevel() == Level.WARN).singleElement().satisfies(event -> {
			assertThat(event.getFormattedMessage()).contains("e1").contains("IllegalStateException");
			// Only the exception type: what a failing call echoes back may concern the applicant.
			assertThat(event.getFormattedMessage()).doesNotContain("503").doesNotContain("19121212");
		});
	}

	@Test
	void aStartThatDoesNotTakeIsAFailureAndDoesNotStopTheBatch() {
		final var notTaking = errand("e1", SLUG_RENEWAL, Duration.ofHours(2));
		final var starting = errand("e2", SLUG_RENEWAL, Duration.ofHours(1));
		whenSelected(notTaking, starting);
		// The starter swallows an engine failure, so the errand comes back from the read after the start without a link.
		whenStillUnstarted(notTaking);
		whenUnstartedThenLinked(starting);
		when(processServiceMock.activeBusinessKeys(MUNICIPALITY_ID, PROCESS_DEFINITION_NAME)).thenReturn(Set.of());
		when(processorMock.assignAndClassify(any())).thenReturn(Outcome.PROCEED);

		final var logs = capturingLogs(() -> assertThat(worker.retryUnstarted())
			.isEqualTo(new FinancialAssistanceProcessStartRetryWorker.Result(2, 1, 0, 0, 1, 0)));

		verify(processorMock, times(2)).startProcess(any());
		assertThat(logs).filteredOn(event -> event.getLevel() == Level.WARN).singleElement()
			.satisfies(event -> assertThat(event.getFormattedMessage()).contains("e1").contains("did not take"));
	}

	@Test
	void startsNothingForAProcessTheEngineCannotBeAskedAbout() {
		final var renewal = errand("r1", SLUG_RENEWAL, Duration.ofHours(1));
		final var newApplication = errand("n1", SLUG_NEW, Duration.ofHours(1));
		whenSelected(renewal, newApplication);
		whenUnstartedThenLinked(newApplication);
		when(processServiceMock.activeBusinessKeys(MUNICIPALITY_ID, PROCESS_DEFINITION_NAME)).thenThrow(new IllegalStateException("engine down"));
		when(processServiceMock.activeBusinessKeys(MUNICIPALITY_ID, PROCESS_DEFINITION_NAME_NEW)).thenReturn(Set.of());
		when(processorMock.assignAndClassify(any())).thenReturn(Outcome.PROCEED);

		final var logs = capturingLogs(() -> assertThat(worker.retryUnstarted())
			.isEqualTo(new FinancialAssistanceProcessStartRetryWorker.Result(2, 1, 0, 0, 1, 0)));

		// No answer is not "none running": the renewal is left for the next run, and the other process still goes ahead.
		final var classified = ArgumentCaptor.forClass(ErrandCreated.class);
		verify(processorMock).assignAndClassify(classified.capture());
		assertThat(classified.getValue().errandId()).isEqualTo("n1");
		verify(errandQueryServiceMock, never()).findErrand(MUNICIPALITY_ID, NAMESPACE, "r1");
		assertThat(logs).filteredOn(event -> event.getLevel() == Level.WARN).singleElement()
			.satisfies(event -> assertThat(event.getFormattedMessage()).contains(PROCESS_DEFINITION_NAME).contains("1 errand"));
	}

	@Test
	void anErrandOlderThanMaxAgeIsNotRetriedButReportedOnce() {
		final var agedOut = errand("old-1", SLUG_RENEWAL, MAX_AGE.plusHours(2));
		whenSelected(agedOut);

		final var firstRun = capturingLogs(() -> assertThat(worker.retryUnstarted())
			.isEqualTo(new FinancialAssistanceProcessStartRetryWorker.Result(0, 0, 0, 0, 0, 1)));
		final var secondRun = capturingLogs(() -> assertThat(worker.retryUnstarted())
			.isEqualTo(new FinancialAssistanceProcessStartRetryWorker.Result(0, 0, 0, 0, 0, 1)));

		// Skipped: the engine is not asked, nothing is classified or started, the errand is not even read again.
		verifyNoInteractions(processServiceMock, processorMock);
		verify(errandQueryServiceMock, never()).findErrand(any(), any(), any());
		assertThat(firstRun).singleElement().satisfies(event -> {
			assertThat(event.getLevel()).isEqualTo(Level.ERROR);
			assertThat(event.getFormattedMessage()).contains("old-1").contains("PT168H").contains("by hand");
		});
		assertThat(secondRun).isEmpty();
	}

	@Test
	void anAgedOutErrandIsReportedAgainOnceTheNodeHasForgottenIt() {
		final var agedOut = errand("old-1", SLUG_RENEWAL, MAX_AGE.plusHours(2));
		final var other = errand("old-2", SLUG_NEW, MAX_AGE.plusHours(3));
		when(errandQueryServiceMock.findWithoutProcessInstance(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(), eq(STATUS_RECEIVED), any(), any()))
			.thenReturn(List.of(agedOut, other))
			.thenReturn(List.of(other))
			.thenReturn(List.of(agedOut, other));

		final var first = capturingLogs(worker::retryUnstarted);
		final var second = capturingLogs(worker::retryUnstarted);
		final var third = capturingLogs(worker::retryUnstarted);

		assertThat(first).extracting(ILoggingEvent::getLevel).containsExactly(Level.ERROR, Level.ERROR);
		assertThat(second).isEmpty();
		// old-1 left the window in the second run (it was started by hand, say), so a third sighting is a new report.
		assertThat(third).singleElement().satisfies(event -> assertThat(event.getFormattedMessage()).contains("old-1"));
	}

	@Test
	void anErrandJustInsideMaxAgeIsRetriedAndOneJustPastItIsNot() {
		final var inside = errand("in", SLUG_RENEWAL, MAX_AGE.minusMinutes(1));
		final var outside = errand("out", SLUG_RENEWAL, MAX_AGE.plusMinutes(1));
		whenSelected(outside, inside);
		whenUnstartedThenLinked(inside);
		when(processServiceMock.activeBusinessKeys(MUNICIPALITY_ID, PROCESS_DEFINITION_NAME)).thenReturn(Set.of());
		when(processorMock.assignAndClassify(any())).thenReturn(Outcome.PROCEED);

		final var result = worker.retryUnstarted();

		assertThat(result).isEqualTo(new FinancialAssistanceProcessStartRetryWorker.Result(1, 1, 0, 0, 0, 1));
		final var classified = ArgumentCaptor.forClass(ErrandCreated.class);
		verify(processorMock).assignAndClassify(classified.capture());
		assertThat(classified.getValue().errandId()).isEqualTo("in");
	}

	@Test
	void doesNothingWhenSwitchedOff() {
		final var result = worker(false).retryUnstarted();

		assertThat(result).isEqualTo(new FinancialAssistanceProcessStartRetryWorker.Result(0, 0, 0, 0, 0, 0));
		verifyNoInteractions(errandQueryServiceMock, processServiceMock, processorMock);
	}

	/** Runs the action with an appender on the worker's logger (level pinned to DEBUG) and returns what it logged. */
	private static List<ILoggingEvent> capturingLogs(final Runnable action) {
		final var logger = (Logger) LoggerFactory.getLogger(FinancialAssistanceProcessStartRetryWorker.class);
		final var originalLevel = logger.getLevel();
		final var appender = new ListAppender<ILoggingEvent>();
		appender.start();
		logger.addAppender(appender);
		logger.setLevel(Level.DEBUG);

		try {
			action.run();
			return List.copyOf(appender.list);
		} finally {
			logger.detachAppender(appender);
			logger.setLevel(originalLevel);
		}
	}
}
