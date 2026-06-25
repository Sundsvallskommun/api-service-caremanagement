package se.sundsvall.caremanagement.types.financialassistance.service.event;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.api.model.PatchErrand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.core.service.event.ErrandCreated;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPerson;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.caremanagement.types.financialassistance.service.DefaultAssigneeService;
import se.sundsvall.caremanagement.types.financialassistance.service.RecentlyClosedErrandService;
import se.sundsvall.caremanagement.types.financialassistance.service.RecentlyClosedErrandService.RecentlyClosed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_NEW;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_RENEWAL;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_SUPPLEMENTARY;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_NEEDS_MANUAL_REVIEW;

@ExtendWith(MockitoExtension.class)
class FinancialAssistanceErrandCreatedListenerTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "errand-1";
	private static final String APPLICANT_PARTY_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";

	@Mock
	private FinancialAssistanceRepository repositoryMock;

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private DefaultAssigneeService defaultAssigneeServiceMock;

	@Mock
	private RecentlyClosedErrandService recentlyClosedErrandServiceMock;

	@Mock
	private FinancialAssistanceProcessStarter processStarterMock;

	@InjectMocks
	private FinancialAssistanceErrandCreatedListener listener;

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
	void ignoresNonEbErrand() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.empty());

		listener.on(event(SLUG_NEW, null));

		verify(repositoryMock).findByErrandId(ERRAND_ID);
		verifyNoMoreInteractions(repositoryMock);
		verifyNoInteractions(processStarterMock, errandServiceMock, defaultAssigneeServiceMock, recentlyClosedErrandServiceMock);
	}

	@Test
	void assignsDefaultHandlaggareForUnassignedNewApplication() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity()));
		when(defaultAssigneeServiceMock.resolve(MUNICIPALITY_ID)).thenReturn(Optional.of("joa01doe"));

		listener.on(event(SLUG_NEW, null));

		final var patchCaptor = ArgumentCaptor.forClass(PatchErrand.class);
		verify(errandServiceMock).updateErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchCaptor.capture());
		assertThat(patchCaptor.getValue().getAssignedUserId()).isEqualTo("joa01doe");
		verifyNoInteractions(processStarterMock); // new applications don't start a process
	}

	@Test
	void skipsDefaultHandlaggareWhenAlreadyAssigned() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity()));

		listener.on(event(SLUG_NEW, "already-assigned"));

		verifyNoInteractions(defaultAssigneeServiceMock, processStarterMock);
		verify(errandServiceMock, never()).updateErrand(any(), any(), any(), any());
	}

	@Test
	void assignsDefaultHandlaggareThenStartsProcessForUnassignedRenewal() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity()));
		when(defaultAssigneeServiceMock.resolve(MUNICIPALITY_ID)).thenReturn(Optional.of("joa01doe"));

		listener.on(event(SLUG_RENEWAL, null));

		final var patchCaptor = ArgumentCaptor.forClass(PatchErrand.class);
		verify(errandServiceMock).updateErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchCaptor.capture());
		assertThat(patchCaptor.getValue().getAssignedUserId()).isEqualTo("joa01doe");
		verify(processStarterMock).start(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any());
	}

	@Test
	void assignsDefaultHandlaggareThenStartsSupplementaryProcessForUnassignedSupplementary() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity()));
		when(defaultAssigneeServiceMock.resolve(MUNICIPALITY_ID)).thenReturn(Optional.of("joa01doe"));

		listener.on(event(SLUG_SUPPLEMENTARY, null));

		final var patchCaptor = ArgumentCaptor.forClass(PatchErrand.class);
		verify(errandServiceMock).updateErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchCaptor.capture());
		assertThat(patchCaptor.getValue().getAssignedUserId()).isEqualTo("joa01doe"); // default is the fallback; actualisation later overwrites with the previous återansökan caseworker
		verify(processStarterMock).startSupplementary(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any());
		verify(processStarterMock, never()).start(any(), any(), any(), any());
	}

	@Test
	void renewalWithExplicitAssigneeStartsProcessWithoutAssigning() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity()));

		listener.on(event(SLUG_RENEWAL));

		verify(processStarterMock).start(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any());
		verifyNoInteractions(defaultAssigneeServiceMock);
		verify(errandServiceMock, never()).updateErrand(any(), any(), any(), any());
	}

	@Test
	void freezesRecentlyClosedReapplicationInsteadOfStartingProcess() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity()));
		when(recentlyClosedErrandServiceMock.findRecentlyClosed(eq(MUNICIPALITY_ID), eq(NAMESPACE), any()))
			.thenReturn(Optional.of(new RecentlyClosed("old-errand", OffsetDateTime.parse("2026-06-20T10:15:30Z"))));

		listener.on(event(SLUG_RENEWAL)); // even a renewal is frozen rather than started

		final var patchCaptor = ArgumentCaptor.forClass(PatchErrand.class);
		verify(errandServiceMock).updateErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchCaptor.capture());
		assertThat(patchCaptor.getValue().getStatus()).isEqualTo(STATUS_NEEDS_MANUAL_REVIEW);
		verifyNoInteractions(processStarterMock);
	}

	@Test
	void freezeStillAssignsDefaultHandlaggare() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity()));
		when(defaultAssigneeServiceMock.resolve(MUNICIPALITY_ID)).thenReturn(Optional.of("joa01doe"));
		when(recentlyClosedErrandServiceMock.findRecentlyClosed(eq(MUNICIPALITY_ID), eq(NAMESPACE), any()))
			.thenReturn(Optional.of(new RecentlyClosed("old-errand", OffsetDateTime.parse("2026-06-20T10:15:30Z"))));

		listener.on(event(SLUG_RENEWAL, null));

		final var patchCaptor = ArgumentCaptor.forClass(PatchErrand.class);
		verify(errandServiceMock, org.mockito.Mockito.times(2)).updateErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchCaptor.capture());
		assertThat(patchCaptor.getAllValues()).anySatisfy(p -> assertThat(p.getAssignedUserId()).isEqualTo("joa01doe"));
		assertThat(patchCaptor.getAllValues()).anySatisfy(p -> assertThat(p.getStatus()).isEqualTo(STATUS_NEEDS_MANUAL_REVIEW));
		verifyNoInteractions(processStarterMock);
	}
}
