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
import se.sundsvall.caremanagement.types.financialassistance.service.event.FinancialAssistanceErrandCreatedProcessor.Outcome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
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

		assertThat(processor.assignAndClassify(event(SLUG_NEW, null))).isEqualTo(Outcome.NOT_EB);

		verify(repositoryMock).findByErrandId(ERRAND_ID);
		verifyNoMoreInteractions(repositoryMock);
		verifyNoInteractions(processStarterMock, errandServiceMock, defaultAssigneeServiceMock, recentlyClosedErrandServiceMock);
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
