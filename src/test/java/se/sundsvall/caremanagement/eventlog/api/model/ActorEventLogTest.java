package se.sundsvall.caremanagement.eventlog.api.model;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActorEventLogTest {

	@Test
	void testAccessors() {
		final var entry = new ErrandEventEntry("ev1", "errand-1", "2281", "ns", "HTTP", "READ", "errands/search", "Såg ärendet i en sökträfflista (1 träffar)",
			"GET", "/2281/ns/errands", "joe001doe", "adAccount", "req-1", 200, OffsetDateTime.parse("2026-09-22T10:00:00Z"));

		final var log = new ActorEventLog(List.of(entry), 4213);

		assertThat(log.events()).containsExactly(entry);
		// The total is separate from the page on purpose — a capped listing must not read as the whole answer.
		assertThat(log.total()).isEqualTo(4213);
	}
}
