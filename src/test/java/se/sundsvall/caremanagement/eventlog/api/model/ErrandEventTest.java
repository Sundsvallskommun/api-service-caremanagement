package se.sundsvall.caremanagement.eventlog.api.model;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrandEventTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@Test
	void componentsAreExposed() {
		final var event = new ErrandEvent(
			"id", "errand-1", "2281", "FINANCIAL_ASSISTANCE", "HTTP",
			"UPDATE", "financial-assistance/calculation/draft/incomes",
			"UPDATE financial-assistance/calculation/draft/incomes",
			"PATCH", "/2281/FINANCIAL_ASSISTANCE/errands/financial-assistance/errand-1/calculation/draft/incomes/row-1",
			"edwmol", "adAccount", "req-1", 200, FIXED_TIMESTAMP);

		assertThat(event.id()).isEqualTo("id");
		assertThat(event.errandId()).isEqualTo("errand-1");
		assertThat(event.municipalityId()).isEqualTo("2281");
		assertThat(event.namespace()).isEqualTo("FINANCIAL_ASSISTANCE");
		assertThat(event.source()).isEqualTo("HTTP");
		assertThat(event.action()).isEqualTo("UPDATE");
		assertThat(event.target()).isEqualTo("financial-assistance/calculation/draft/incomes");
		assertThat(event.description()).isEqualTo("UPDATE financial-assistance/calculation/draft/incomes");
		assertThat(event.httpMethod()).isEqualTo("PATCH");
		assertThat(event.requestPath()).isEqualTo("/2281/FINANCIAL_ASSISTANCE/errands/financial-assistance/errand-1/calculation/draft/incomes/row-1");
		assertThat(event.actor()).isEqualTo("edwmol");
		assertThat(event.actorType()).isEqualTo("adAccount");
		assertThat(event.requestId()).isEqualTo("req-1");
		assertThat(event.statusCode()).isEqualTo(200);
		assertThat(event.created()).isEqualTo(FIXED_TIMESTAMP);
	}
}
