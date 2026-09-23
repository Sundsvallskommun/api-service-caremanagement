package se.sundsvall.caremanagement.eventlog.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import se.sundsvall.caremanagement.eventlog.api.model.LifecareAccess;
import se.sundsvall.caremanagement.eventlog.integration.db.ErrandEventRepository;
import se.sundsvall.caremanagement.eventlog.integration.db.model.ErrandEventEntity;
import se.sundsvall.caremanagement.shared.ErrandAccessGuard;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.support.Identifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class ErrandEventServiceTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@Mock
	private ErrandEventRepository repositoryMock;

	@Mock
	private ErrandAccessGuard errandAccessGuardMock;

	@InjectMocks
	private ErrandEventService service;

	@Test
	void recordStampsCreatedAndSaves() {
		final var entity = ErrandEventEntity.create().withErrandId("e1").withAction("READ").withTarget("errand");

		service.recordEvent(entity);

		verify(repositoryMock).save(entity);
		assertThat(entity.getCreated()).isNotNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	void recordLifecareAccessesWritesOneLifecareRowPerAccessAttributedToTheCaller() {
		final var caller = Identifier.parse("joe001doe; type=adAccount");
		final var accesses = List.of(
			LifecareAccess.create().withAction("READ").withTarget("lifecare/journal-notes").withDescription("Läste journalen i Lifecare"),
			LifecareAccess.create().withAction("CREATE").withTarget("lifecare/journal-notes").withLifecareId("4711"));

		service.recordLifecareAccesses("2281", "ns", "e1", caller, accesses);

		verify(errandAccessGuardMock).verifyExistingErrand("2281", "ns", "e1");
		final ArgumentCaptor<List<ErrandEventEntity>> captor = ArgumentCaptor.forClass(List.class);
		verify(repositoryMock).saveAll(captor.capture());
		assertThat(captor.getValue())
			.extracting(ErrandEventEntity::getErrandId, ErrandEventEntity::getMunicipalityId, ErrandEventEntity::getNamespace, ErrandEventEntity::getSource,
				ErrandEventEntity::getAction, ErrandEventEntity::getTarget, ErrandEventEntity::getDescription, ErrandEventEntity::getLifecareId,
				ErrandEventEntity::getActor, ErrandEventEntity::getActorType)
			.containsExactly(
				tuple("e1", "2281", "ns", "LIFECARE", "READ", "lifecare/journal-notes", "Läste journalen i Lifecare", null, "joe001doe", "adAccount"),
				tuple("e1", "2281", "ns", "LIFECARE", "CREATE", "lifecare/journal-notes", "CREATE lifecare/journal-notes", "4711", "joe001doe", "adAccount"));
		assertThat(captor.getValue()).allSatisfy(entity -> assertThat(entity.getCreated()).isNotNull());
	}

	@Test
	void recordLifecareAccessesOnUnknownErrandRecordsNothing() {
		doThrow(Problem.valueOf(NOT_FOUND, "No errand")).when(errandAccessGuardMock).verifyExistingErrand("2281", "ns", "e1");
		final var accesses = List.of(LifecareAccess.create().withAction("READ").withTarget("lifecare/reminders"));
		final var caller = Identifier.parse("joe001doe; type=adAccount");

		assertThatThrownBy(() -> service.recordLifecareAccesses("2281", "ns", "e1", caller, accesses))
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
		verify(repositoryMock, never()).saveAll(anyList());
	}

	@Test
	void recordDomainEventSavesAsIsWithoutStampingCreated() {
		final var entity = ErrandEventEntity.create().withErrandId("e1").withAction("CREATE").withTarget("errand").withCreated(FIXED_TIMESTAMP);

		service.recordDomainEvent(entity);

		verify(repositoryMock).save(entity);
		assertThat(entity.getCreated()).isEqualTo(FIXED_TIMESTAMP);
	}

	@Test
	void deleteForErrandDisposesTheTenantScopedLog() {
		when(repositoryMock.deleteByErrandIdAndMunicipalityIdAndNamespace("e1", "2281", "ns")).thenReturn(4);

		service.deleteForErrand("2281", "ns", "e1");

		verify(repositoryMock).deleteByErrandIdAndMunicipalityIdAndNamespace("e1", "2281", "ns");
	}

	@Test
	void listForErrandMapsRepositoryResultsAndForwardsNoFilters() {
		when(repositoryMock.findFiltered("2281", "ns", "e1", null, null, null, true)).thenReturn(List.of(
			event("ev2", "READ", "joe001doe", FIXED_TIMESTAMP),
			event("ev1", "UPDATE", "edwmol", FIXED_TIMESTAMP.minusHours(1))));

		final var result = service.listForErrand("2281", "ns", "e1", null, null, null, true);

		assertThat(result).extracting("id", "action", "actor")
			.containsExactly(
				tuple("ev2", "READ", "joe001doe"),
				tuple("ev1", "UPDATE", "edwmol"));
		verify(repositoryMock).findFiltered("2281", "ns", "e1", null, null, null, true);
	}

	@Test
	void listForActorReadsAcrossErrandsAndReportsTheTotalBesideTheCappedPage() {
		final var from = OffsetDateTime.parse("2026-09-01T00:00:00Z");
		final var to = OffsetDateTime.parse("2026-10-01T00:00:00Z");
		when(repositoryMock.findByActor(eq("2281"), eq("ns"), eq("joe001doe"), eq("read"), eq("http"), eq(from), eq(to), any(Pageable.class)))
			.thenReturn(List.of(event("ev2", "READ", "joe001doe", FIXED_TIMESTAMP)));
		when(repositoryMock.countByActor("2281", "ns", "joe001doe", "read", "http", from, to)).thenReturn(4213L);

		final var result = service.listForActor("2281", "ns", "joe001doe", "read", "http", from, to);

		assertThat(result.events()).extracting("id", "action", "actor").containsExactly(tuple("ev2", "READ", "joe001doe"));
		// A follow-up that shows one row out of 4213 and does not say so is worse than showing nothing.
		assertThat(result.total()).isEqualTo(4213);
	}

	@Test
	void listForActorCapsThePage() {
		when(repositoryMock.findByActor(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(List.of());

		service.listForActor("2281", "ns", "joe001doe", null, null, null, null);

		final var pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(repositoryMock).findByActor(eq("2281"), eq("ns"), eq("joe001doe"), isNull(), isNull(), isNull(), isNull(), pageable.capture());
		assertThat(pageable.getValue().getPageSize()).isEqualTo(1000);
		assertThat(pageable.getValue().getPageNumber()).isZero();
	}

	@Test
	void listForErrandForwardsAllFiltersToRepository() {
		when(repositoryMock.findFiltered("2281", "ns", "e1", "read", "edwmol", "event", false)).thenReturn(List.of());

		service.listForErrand("2281", "ns", "e1", "read", "edwmol", "event", false);

		verify(repositoryMock).findFiltered("2281", "ns", "e1", "read", "edwmol", "event", false);
	}

	@Test
	void listForErrandReturnsEmptyWhenNone() {
		when(repositoryMock.findFiltered("2281", "ns", "e2", null, null, null, true)).thenReturn(List.of());

		assertThat(service.listForErrand("2281", "ns", "e2", null, null, null, true)).isEmpty();
	}

	@Test
	void countForErrandDelegatesToRepositoryWithoutFilters() {
		when(repositoryMock.countFiltered("2281", "ns", "e1", null, null, null, true)).thenReturn(7L);

		assertThat(service.countForErrand("2281", "ns", "e1", null, null, null, true)).isEqualTo(7L);
		verify(repositoryMock).countFiltered("2281", "ns", "e1", null, null, null, true);
	}

	@Test
	void countForErrandForwardsFiltersToRepository() {
		when(repositoryMock.countFiltered("2281", "ns", "e1", null, null, null, false)).thenReturn(1L);

		assertThat(service.countForErrand("2281", "ns", "e1", null, null, null, false)).isEqualTo(1L);
		verify(repositoryMock).countFiltered("2281", "ns", "e1", null, null, null, false);
	}

	private static ErrandEventEntity event(final String id, final String action, final String actor, final OffsetDateTime created) {
		return ErrandEventEntity.create()
			.withId(id)
			.withErrandId("e1")
			.withMunicipalityId("2281")
			.withNamespace("FINANCIAL_ASSISTANCE")
			.withSource("HTTP")
			.withAction(action)
			.withTarget("errand")
			.withDescription(action + " errand")
			.withHttpMethod("GET")
			.withRequestPath("/2281/FINANCIAL_ASSISTANCE/errands/e1")
			.withActor(actor)
			.withActorType(Optional.ofNullable(actor).map(_ -> "adAccount").orElse(null))
			.withRequestId("req")
			.withStatusCode(200)
			.withCreated(created);
	}
}
