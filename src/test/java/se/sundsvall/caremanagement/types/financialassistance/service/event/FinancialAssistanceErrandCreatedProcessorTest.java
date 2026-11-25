package se.sundsvall.caremanagement.types.financialassistance.service.event;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.core.api.model.PatchErrand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.core.service.event.ErrandCreated;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseService;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaChild;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPerson;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.caremanagement.types.financialassistance.service.DefaultAssigneeService;
import se.sundsvall.caremanagement.types.financialassistance.service.ProtectedIdentityGate;
import se.sundsvall.caremanagement.types.financialassistance.service.RecentlyClosedErrandService;
import se.sundsvall.caremanagement.types.financialassistance.service.RecentlyClosedErrandService.RecentlyClosed;
import se.sundsvall.caremanagement.types.financialassistance.service.event.FinancialAssistanceErrandCreatedProcessor.Outcome;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_NEW;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_RENEWAL;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_SUPPLEMENTARY;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_NEEDS_MANUAL_REVIEW;

@ExtendWith(MockitoExtension.class)
class FinancialAssistanceErrandCreatedProcessorTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "errand-1";
	private static final String APPLICANT_PARTY_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
	private static final String CO_APPLICANT_PARTY_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
	private static final String CHILD_PARTY_ID = "0b9d2f6e-3c1a-4e57-8d20-6a4f1c9e7b31";

	@Mock
	private FinancialAssistanceRepository repositoryMock;

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private DefaultAssigneeService defaultAssigneeServiceMock;

	@Mock
	private ProtectedIdentityGate protectedIdentityGateMock;

	@Mock
	private RecentlyClosedErrandService recentlyClosedErrandServiceMock;

	@Mock
	private FinancialAssistanceProcessStarter processStarterMock;

	@InjectMocks
	private FinancialAssistanceErrandCreatedProcessor processor;

	private static ErrandCreated event(final String typeSlug) {
		return event(typeSlug, "assignee");
	}

	private static ErrandCreated event(final String typeSlug, final String assignedUserId) {
		return new ErrandCreated(ERRAND_ID, typeSlug, MUNICIPALITY_ID, NAMESPACE, "reporter", assignedUserId,
			OffsetDateTime.parse("2026-06-05T12:00:00Z"));
	}

	private static FinancialAssistanceEntity entity() {
		return FinancialAssistanceEntity.create()
			.withErrandId(ERRAND_ID)
			.withPersons(List.of(FaPerson.create().withRole("APPLICANT").withPartyId(APPLICANT_PARTY_ID)));
	}

	@Test
	void assignAndClassifyReturnsNotEbForNonEbErrand() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.empty());

		assertThat(processor.assignAndClassify(event(SLUG_NEW, null))).isEqualTo(Outcome.NOT_FINANCIAL_ASSISTANCE);

		verify(repositoryMock).findByErrandId(ERRAND_ID);
		verifyNoMoreInteractions(repositoryMock);
		verifyNoInteractions(processStarterMock, errandServiceMock, defaultAssigneeServiceMock, protectedIdentityGateMock, recentlyClosedErrandServiceMock);
	}

	@Test
	void assignAndClassifyAssignsDefaultHandlaggareForUnassigned() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity()));
		when(defaultAssigneeServiceMock.resolve(MUNICIPALITY_ID)).thenReturn(Optional.of("joa01doe"));

		assertThat(processor.assignAndClassify(event(SLUG_NEW, null))).isEqualTo(Outcome.PROCEED);

		final var patchCaptor = ArgumentCaptor.forClass(PatchErrand.class);
		verify(errandServiceMock).updateErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchCaptor.capture());
		assertThat(patchCaptor.getValue().getAssignedUserId()).isEqualTo("joa01doe");
		verifyNoInteractions(processStarterMock);
	}

	@Test
	void assignAndClassifySkipsDefaultHandlaggareWhenAlreadyAssigned() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity()));

		assertThat(processor.assignAndClassify(event(SLUG_NEW, "already-assigned"))).isEqualTo(Outcome.PROCEED);

		verifyNoInteractions(defaultAssigneeServiceMock);
		verify(errandServiceMock, never()).updateErrand(any(), any(), any(), any());
	}

	@Test
	void assignAndClassifyFreezesRecentlyClosedReapplication() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity()));
		when(recentlyClosedErrandServiceMock.findRecentlyClosed(eq(MUNICIPALITY_ID), eq(NAMESPACE), any()))
			.thenReturn(Optional.of(new RecentlyClosed("old-errand", OffsetDateTime.parse("2026-06-20T10:15:30Z"))));

		assertThat(processor.assignAndClassify(event(SLUG_RENEWAL))).isEqualTo(Outcome.FROZEN); // even a renewal is frozen rather than started

		final var patchCaptor = ArgumentCaptor.forClass(PatchErrand.class);
		verify(errandServiceMock).updateErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchCaptor.capture());
		assertThat(patchCaptor.getValue().getStatus()).isEqualTo(STATUS_NEEDS_MANUAL_REVIEW);
		verifyNoInteractions(processStarterMock);
	}

	@Test
	void assignAndClassifyFreezeStillAssignsDefaultHandlaggare() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity()));
		when(defaultAssigneeServiceMock.resolve(MUNICIPALITY_ID)).thenReturn(Optional.of("joa01doe"));
		when(recentlyClosedErrandServiceMock.findRecentlyClosed(eq(MUNICIPALITY_ID), eq(NAMESPACE), any()))
			.thenReturn(Optional.of(new RecentlyClosed("old-errand", OffsetDateTime.parse("2026-06-20T10:15:30Z"))));

		assertThat(processor.assignAndClassify(event(SLUG_RENEWAL, null))).isEqualTo(Outcome.FROZEN);

		final var patchCaptor = ArgumentCaptor.forClass(PatchErrand.class);
		verify(errandServiceMock, times(2)).updateErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchCaptor.capture());
		assertThat(patchCaptor.getAllValues()).anySatisfy(p -> assertThat(p.getAssignedUserId()).isEqualTo("joa01doe"));
		assertThat(patchCaptor.getAllValues()).anySatisfy(p -> assertThat(p.getStatus()).isEqualTo(STATUS_NEEDS_MANUAL_REVIEW));
		verifyNoInteractions(processStarterMock);
	}

	// ---- Protected identity freeze (fail closed) ------------------------------------------------------------------------

	@Test
	void assignAndClassifyFreezesWhenAPartyIsProtectedOrUnknown() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity()));
		when(protectedIdentityGateMock.protectedOrUnknown(eq(MUNICIPALITY_ID), eq(ERRAND_ID), anyCollection())).thenReturn(true);

		assertThat(processor.assignAndClassify(event(SLUG_NEW))).isEqualTo(Outcome.FROZEN);

		final var patchCaptor = ArgumentCaptor.forClass(PatchErrand.class);
		verify(errandServiceMock).updateErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchCaptor.capture());
		assertThat(patchCaptor.getValue().getStatus()).isEqualTo(STATUS_NEEDS_MANUAL_REVIEW);
		verifyNoInteractions(processStarterMock);
	}

	@Test
	void assignAndClassifyProtectedFreezeDoesNotConsultRecentlyClosed() {
		// The protected-identity freeze is checked first and is terminal, so the recently-closed lookup is never made.
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity()));
		when(protectedIdentityGateMock.protectedOrUnknown(eq(MUNICIPALITY_ID), eq(ERRAND_ID), anyCollection())).thenReturn(true);

		assertThat(processor.assignAndClassify(event(SLUG_RENEWAL))).isEqualTo(Outcome.FROZEN);

		verifyNoInteractions(recentlyClosedErrandServiceMock, processStarterMock);
	}

	@Test
	void assignAndClassifyProtectedFreezeStillAssignsDefaultHandlaggare() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity()));
		when(defaultAssigneeServiceMock.resolve(MUNICIPALITY_ID)).thenReturn(Optional.of("joa01doe"));
		when(protectedIdentityGateMock.protectedOrUnknown(eq(MUNICIPALITY_ID), eq(ERRAND_ID), anyCollection())).thenReturn(true);

		assertThat(processor.assignAndClassify(event(SLUG_NEW, null))).isEqualTo(Outcome.FROZEN);

		final var patchCaptor = ArgumentCaptor.forClass(PatchErrand.class);
		verify(errandServiceMock, times(2)).updateErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchCaptor.capture());
		assertThat(patchCaptor.getAllValues()).anySatisfy(p -> assertThat(p.getAssignedUserId()).isEqualTo("joa01doe"));
		assertThat(patchCaptor.getAllValues()).anySatisfy(p -> assertThat(p.getStatus()).isEqualTo(STATUS_NEEDS_MANUAL_REVIEW));
		verifyNoInteractions(processStarterMock);
	}

	@Test
	void assignAndClassifyChecksApplicantCoApplicantAndChildrenOnce() {
		final var household = FinancialAssistanceEntity.create()
			.withErrandId(ERRAND_ID)
			.withPersons(List.of(
				FaPerson.create().withRole("APPLICANT").withPartyId(APPLICANT_PARTY_ID),
				FaPerson.create().withRole("CO_APPLICANT").withPartyId(CO_APPLICANT_PARTY_ID),
				FaPerson.create().withRole("CO_APPLICANT").withPartyId(" ")))
			.withChildren(List.of(
				FaChild.create().withPartyId(CHILD_PARTY_ID),
				FaChild.create().withPartyId(APPLICANT_PARTY_ID), // repeated id — read once
				FaChild.create().withFirstName("No id")));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(household));
		when(recentlyClosedErrandServiceMock.findRecentlyClosed(eq(MUNICIPALITY_ID), eq(NAMESPACE), any())).thenReturn(Optional.empty());

		assertThat(processor.assignAndClassify(event(SLUG_NEW))).isEqualTo(Outcome.PROCEED);

		@SuppressWarnings("unchecked")
		final ArgumentCaptor<Collection<String>> partiesCaptor = ArgumentCaptor.forClass(Collection.class);
		verify(protectedIdentityGateMock).protectedOrUnknown(eq(MUNICIPALITY_ID), eq(ERRAND_ID), partiesCaptor.capture());
		assertThat(partiesCaptor.getValue()).containsExactly(APPLICANT_PARTY_ID, CO_APPLICANT_PARTY_ID, CHILD_PARTY_ID);
	}

	@Test
	void assignAndClassifyChecksProtectedIdentityBeforeRecentlyClosedWhenNeitherFreezes() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity()));
		when(protectedIdentityGateMock.protectedOrUnknown(eq(MUNICIPALITY_ID), eq(ERRAND_ID), anyCollection())).thenReturn(false);
		when(recentlyClosedErrandServiceMock.findRecentlyClosed(eq(MUNICIPALITY_ID), eq(NAMESPACE), any())).thenReturn(Optional.empty());

		assertThat(processor.assignAndClassify(event(SLUG_RENEWAL))).isEqualTo(Outcome.PROCEED);

		final InOrder order = inOrder(protectedIdentityGateMock, recentlyClosedErrandServiceMock);
		order.verify(protectedIdentityGateMock).protectedOrUnknown(eq(MUNICIPALITY_ID), eq(ERRAND_ID), anyCollection());
		order.verify(recentlyClosedErrandServiceMock).findRecentlyClosed(eq(MUNICIPALITY_ID), eq(NAMESPACE), any());
		verify(errandServiceMock, never()).updateErrand(any(), any(), any(), any());
		verifyNoInteractions(processStarterMock);
	}

	@Test
	void assignAndClassifyStillFreezesRecentlyClosedWhenNobodyIsProtected() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity()));
		when(protectedIdentityGateMock.protectedOrUnknown(eq(MUNICIPALITY_ID), eq(ERRAND_ID), anyCollection())).thenReturn(false);
		when(recentlyClosedErrandServiceMock.findRecentlyClosed(eq(MUNICIPALITY_ID), eq(NAMESPACE), any()))
			.thenReturn(Optional.of(new RecentlyClosed("old-errand", OffsetDateTime.parse("2026-06-20T10:15:30Z"))));

		assertThat(processor.assignAndClassify(event(SLUG_RENEWAL))).isEqualTo(Outcome.FROZEN);

		final var patchCaptor = ArgumentCaptor.forClass(PatchErrand.class);
		verify(errandServiceMock).updateErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchCaptor.capture());
		assertThat(patchCaptor.getValue().getStatus()).isEqualTo(STATUS_NEEDS_MANUAL_REVIEW);
	}

	@Test
	void assignAndClassifyFreezesWhenTheIdentityLookupFailsInTheRealGate() {
		// Wired with the real gate: a failing Citizen/Lifecare lookup must end in FROZEN, not in PROCEED and not in an
		// exception that would leave the errand unclassified.
		final var citizenServiceMock = mock(CitizenService.class);
		final var lifecareCaseServiceMock = mock(LifecareCaseService.class);
		final var failClosedProcessor = new FinancialAssistanceErrandCreatedProcessor(repositoryMock, errandServiceMock, defaultAssigneeServiceMock,
			new ProtectedIdentityGate(citizenServiceMock, lifecareCaseServiceMock), recentlyClosedErrandServiceMock, processStarterMock);
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity()));
		when(citizenServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenThrow(Problem.valueOf(BAD_GATEWAY, "citizen down"));

		assertThat(failClosedProcessor.assignAndClassify(event(SLUG_NEW))).isEqualTo(Outcome.FROZEN);

		final var patchCaptor = ArgumentCaptor.forClass(PatchErrand.class);
		verify(errandServiceMock).updateErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchCaptor.capture());
		assertThat(patchCaptor.getValue().getStatus()).isEqualTo(STATUS_NEEDS_MANUAL_REVIEW);
		verifyNoInteractions(recentlyClosedErrandServiceMock, processStarterMock);
	}

	@Test
	void assignAndClassifyProceedsWhenTheRealGateClearsEveryone() {
		final var citizenServiceMock = mock(CitizenService.class);
		final var lifecareCaseServiceMock = mock(LifecareCaseService.class);
		final var clearedProcessor = new FinancialAssistanceErrandCreatedProcessor(repositoryMock, errandServiceMock, defaultAssigneeServiceMock,
			new ProtectedIdentityGate(citizenServiceMock, lifecareCaseServiceMock), recentlyClosedErrandServiceMock, processStarterMock);
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity()));
		when(citizenServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(false);
		when(lifecareCaseServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(false);
		when(recentlyClosedErrandServiceMock.findRecentlyClosed(eq(MUNICIPALITY_ID), eq(NAMESPACE), any())).thenReturn(Optional.empty());

		assertThat(clearedProcessor.assignAndClassify(event(SLUG_NEW))).isEqualTo(Outcome.PROCEED);

		verify(errandServiceMock, never()).updateErrand(any(), any(), any(), any());
	}

	@Test
	void assignAndClassifyProtectedFreezeLogsOnlyANeutralLineWithTheErrandId() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity()));
		when(protectedIdentityGateMock.protectedOrUnknown(eq(MUNICIPALITY_ID), eq(ERRAND_ID), anyCollection())).thenReturn(true);

		final var events = capturingLogs(() -> processor.assignAndClassify(event(SLUG_NEW)));

		assertThat(events).singleElement().satisfies(logged -> {
			assertThat(logged.getLevel()).isEqualTo(Level.INFO);
			assertThat(logged.getFormattedMessage())
				.contains(ERRAND_ID)
				.doesNotContain(APPLICANT_PARTY_ID)
				.doesNotContainIgnoringCase("protected")
				.doesNotContainIgnoringCase("skyddad");
		});
	}

	/** Runs the action with an appender on the processor's logger (level pinned to DEBUG) and returns what it logged. */
	private static List<ILoggingEvent> capturingLogs(final Supplier<Outcome> action) {
		final var logger = (Logger) LoggerFactory.getLogger(FinancialAssistanceErrandCreatedProcessor.class);
		final var originalLevel = logger.getLevel();
		final var appender = new ListAppender<ILoggingEvent>();
		appender.start();
		logger.addAppender(appender);
		logger.setLevel(Level.DEBUG);

		try {
			action.get();
			return List.copyOf(appender.list);
		} finally {
			logger.setLevel(originalLevel);
			logger.detachAppender(appender);
		}
	}

	@Test
	void startProcessStartsRenewalProcess() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity()));

		processor.startProcess(event(SLUG_RENEWAL));

		verify(processStarterMock).startFor(eq(SLUG_RENEWAL), eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any());
	}

	@Test
	void startProcessStartsSupplementaryProcess() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity()));

		processor.startProcess(event(SLUG_SUPPLEMENTARY));

		verify(processStarterMock).startFor(eq(SLUG_SUPPLEMENTARY), eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any());
	}

	@Test
	void startProcessStartsNewProcess() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity()));

		processor.startProcess(event(SLUG_NEW));

		verify(processStarterMock).startFor(eq(SLUG_NEW), eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any());
	}

	@Test
	void startProcessDoesNothingForNonEbErrand() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.empty());

		processor.startProcess(event(SLUG_RENEWAL));

		verifyNoInteractions(processStarterMock);
	}
}
