package se.sundsvall.caremanagement.eventlog.service;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.eventlog.integration.db.ErrandEventRepository;
import se.sundsvall.caremanagement.eventlog.integration.db.model.ErrandEventEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrandEventServiceTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@Mock
	private ErrandEventRepository repositoryMock;

	@InjectMocks
	private ErrandEventService service;

	@Test
	void recordStampsCreatedAndSaves() {
		final var entity = ErrandEventEntity.create().withErrandId("e1").withAction("READ").withTarget("errand");

		service.record(entity);

		verify(repositoryMock).save(entity);
		assertThat(entity.getCreated()).isNotNull();
	}

	@Test
	void recordDomainEventSavesAsIsWithoutStampingCreated() {
		final var entity = ErrandEventEntity.create().withErrandId("e1").withAction("CREATE").withTarget("errand").withCreated(FIXED_TIMESTAMP);

		service.recordDomainEvent(entity);

		verify(repositoryMock).save(entity);
		assertThat(entity.getCreated()).isEqualTo(FIXED_TIMESTAMP);
	}

	@Test
	void listForErrandMapsAllWhenNoFilters() {
		when(repositoryMock.findByErrandIdOrderByCreatedDesc("e1")).thenReturn(List.of(
			event("ev2", "READ", "joe001doe", FIXED_TIMESTAMP),
			event("ev1", "UPDATE", "edwmol", FIXED_TIMESTAMP.minusHours(1))));

		final var result = service.listForErrand("e1", null, null, null, true);

		assertThat(result).extracting("id", "action", "actor")
			.containsExactly(
				tuple("ev2", "READ", "joe001doe"),
				tuple("ev1", "UPDATE", "edwmol"));
		verify(repositoryMock).findByErrandIdOrderByCreatedDesc("e1");
	}

	@Test
	void listForErrandFiltersByActionCaseInsensitively() {
		when(repositoryMock.findByErrandIdOrderByCreatedDesc("e1")).thenReturn(List.of(
			event("ev2", "READ", "joe001doe", FIXED_TIMESTAMP),
			event("ev1", "UPDATE", "edwmol", FIXED_TIMESTAMP.minusHours(1))));

		final var result = service.listForErrand("e1", "read", null, null, true);

		assertThat(result).extracting("id").containsExactly("ev2");
	}

	@Test
	void listForErrandFiltersByActor() {
		when(repositoryMock.findByErrandIdOrderByCreatedDesc("e1")).thenReturn(List.of(
			event("ev2", "READ", "joe001doe", FIXED_TIMESTAMP),
			event("ev1", "UPDATE", "edwmol", FIXED_TIMESTAMP.minusHours(1)),
			event("ev0", "READ", null, FIXED_TIMESTAMP.minusHours(2))));

		final var result = service.listForErrand("e1", null, "edwmol", null, true);

		assertThat(result).extracting("id").containsExactly("ev1");
	}

	@Test
	void listForErrandExcludesReadsWhenAsked() {
		when(repositoryMock.findByErrandIdOrderByCreatedDesc("e1")).thenReturn(List.of(
			event("ev2", "READ", "joe001doe", FIXED_TIMESTAMP),
			event("ev1", "UPDATE", "edwmol", FIXED_TIMESTAMP.minusHours(1))));

		final var result = service.listForErrand("e1", null, null, null, false);

		assertThat(result).extracting("id").containsExactly("ev1");
	}

	@Test
	void listForErrandFiltersBySource() {
		when(repositoryMock.findByErrandIdOrderByCreatedDesc("e1")).thenReturn(List.of(
			sourced("ev2", "EVENT", FIXED_TIMESTAMP),
			sourced("ev1", "HTTP", FIXED_TIMESTAMP.minusHours(1))));

		final var result = service.listForErrand("e1", null, null, "event", true);

		assertThat(result).extracting("id").containsExactly("ev2");
	}

	@Test
	void listForErrandReturnsEmptyWhenNone() {
		when(repositoryMock.findByErrandIdOrderByCreatedDesc("e2")).thenReturn(List.of());

		assertThat(service.listForErrand("e2", null, null, null, true)).isEmpty();
	}

	@Test
	void countForErrandCountsAllWhenNoFilters() {
		when(repositoryMock.findByErrandIdOrderByCreatedDesc("e1")).thenReturn(List.of(
			event("ev2", "READ", "joe001doe", FIXED_TIMESTAMP),
			event("ev1", "UPDATE", "edwmol", FIXED_TIMESTAMP.minusHours(1))));

		assertThat(service.countForErrand("e1", null, null, null, true)).isEqualTo(2);
	}

	@Test
	void countForErrandExcludesReadsWhenAsked() {
		when(repositoryMock.findByErrandIdOrderByCreatedDesc("e1")).thenReturn(List.of(
			event("ev2", "READ", "joe001doe", FIXED_TIMESTAMP),
			event("ev1", "UPDATE", "edwmol", FIXED_TIMESTAMP.minusHours(1))));

		assertThat(service.countForErrand("e1", null, null, null, false)).isEqualTo(1);
	}

	private static ErrandEventEntity sourced(final String id, final String source, final OffsetDateTime created) {
		return event(id, "READ", "joe001doe", created).withSource(source);
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
			.withActorType(actor == null ? null : "adAccount")
			.withRequestId("req")
			.withStatusCode(200)
			.withCreated(created);
	}
}
