package se.sundsvall.caremanagement.types.financialassistance.service.event;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.service.event.ErrandStatusChanged;
import se.sundsvall.caremanagement.core.spi.ErrandQueryService;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_NEW;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_RENEWAL;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_SUPPLEMENTARY;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_NEEDS_MANUAL_REVIEW;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_RECEIVED;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_UNDER_REVIEW;

@ExtendWith(MockitoExtension.class)
class FinancialAssistanceReleaseListenerTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "errand-1";

	@Mock
	private FinancialAssistanceRepository repositoryMock;

	@Mock
	private ErrandQueryService errandQueryServiceMock;

	@Mock
	private FinancialAssistanceProcessStarter processStarterMock;

	@InjectMocks
	private FinancialAssistanceReleaseListener listener;

	private static ErrandStatusChanged event(final String fromStatus, final String toStatus) {
		return event(SLUG_RENEWAL, fromStatus, toStatus);
	}

	private static ErrandStatusChanged event(final String typeSlug, final String fromStatus, final String toStatus) {
		return new ErrandStatusChanged(ERRAND_ID, typeSlug, MUNICIPALITY_ID, NAMESPACE, fromStatus, toStatus, "joa01doe",
			OffsetDateTime.parse("2026-06-25T09:00:00Z"));
	}

	@Test
	void startsProcessOnManualReviewRelease() {
		when(errandQueryServiceMock.findErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Optional.of(Errand.create().withId(ERRAND_ID))); // no process instance yet
		final var entity = FinancialAssistanceEntity.create().withErrandId(ERRAND_ID);
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity));

		listener.on(event(STATUS_NEEDS_MANUAL_REVIEW, STATUS_UNDER_REVIEW));

		verify(processStarterMock).startFor(SLUG_RENEWAL, MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, entity);
	}

	/**
	 * The recently-closed freeze is party-based and fires for any application type, so a released errand must resume in
	 * its own type's flow. A household that applied alone before and now applies as a couple is a nyansökan (the
	 * eligibility check routes it as one) — releasing it must not run it as a återansökan.
	 */
	@Test
	void releasedNewApplicationStartsItsOwnProcessRatherThanTheRenewalOne() {
		when(errandQueryServiceMock.findErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Optional.of(Errand.create().withId(ERRAND_ID)));
		final var entity = FinancialAssistanceEntity.create().withErrandId(ERRAND_ID);
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity));

		listener.on(event(SLUG_NEW, STATUS_NEEDS_MANUAL_REVIEW, STATUS_UNDER_REVIEW));

		verify(processStarterMock).startFor(SLUG_NEW, MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, entity);
	}

	@Test
	void releasedSupplementaryApplicationStartsItsOwnProcess() {
		when(errandQueryServiceMock.findErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Optional.of(Errand.create().withId(ERRAND_ID)));
		final var entity = FinancialAssistanceEntity.create().withErrandId(ERRAND_ID);
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity));

		listener.on(event(SLUG_SUPPLEMENTARY, STATUS_NEEDS_MANUAL_REVIEW, STATUS_UNDER_REVIEW));

		verify(processStarterMock).startFor(SLUG_SUPPLEMENTARY, MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, entity);
	}

	@Test
	void ignoresNonReleaseTransition() {
		// A normal renewal moves RECEIVED → UNDER_REVIEW (process worker) — must NOT re-trigger a start.
		listener.on(event(STATUS_RECEIVED, STATUS_UNDER_REVIEW));

		verifyNoInteractions(errandQueryServiceMock, repositoryMock, processStarterMock);
	}

	@Test
	void skipsWhenErrandAlreadyHasProcessInstance() {
		when(errandQueryServiceMock.findErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Optional.of(Errand.create().withId(ERRAND_ID).withProcessInstanceId("already-running")));

		listener.on(event(STATUS_NEEDS_MANUAL_REVIEW, STATUS_UNDER_REVIEW));

		verifyNoInteractions(processStarterMock);
	}

	@Test
	void noopWhenErrandEnvelopeMissing() {
		when(errandQueryServiceMock.findErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Optional.empty());

		listener.on(event(STATUS_NEEDS_MANUAL_REVIEW, STATUS_UNDER_REVIEW));

		verifyNoInteractions(processStarterMock);
		verify(repositoryMock, never()).findByErrandId(any());
	}

	@Test
	void noopWhenTypedDataMissing() {
		when(errandQueryServiceMock.findErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Optional.of(Errand.create().withId(ERRAND_ID)));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.empty());

		listener.on(event(STATUS_NEEDS_MANUAL_REVIEW, STATUS_UNDER_REVIEW));

		verifyNoInteractions(processStarterMock);
	}
}
