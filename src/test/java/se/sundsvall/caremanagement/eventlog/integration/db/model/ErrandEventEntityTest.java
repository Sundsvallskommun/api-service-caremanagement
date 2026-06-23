package se.sundsvall.caremanagement.eventlog.integration.db.model;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrandEventEntityTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@Test
	void builderMethods() {
		final var entity = ErrandEventEntity.create()
			.withId("id")
			.withErrandId("errand-1")
			.withMunicipalityId("2281")
			.withNamespace("FINANCIAL_ASSISTANCE")
			.withSource("HTTP")
			.withAction("READ")
			.withTarget("errand")
			.withDescription("READ errand")
			.withHttpMethod("GET")
			.withRequestPath("/2281/FINANCIAL_ASSISTANCE/errands/errand-1")
			.withActor("joe001doe")
			.withActorType("adAccount")
			.withRequestId("req-1")
			.withStatusCode(200)
			.withCreated(FIXED_TIMESTAMP);

		assertThat(entity.getId()).isEqualTo("id");
		assertThat(entity.getErrandId()).isEqualTo("errand-1");
		assertThat(entity.getMunicipalityId()).isEqualTo("2281");
		assertThat(entity.getNamespace()).isEqualTo("FINANCIAL_ASSISTANCE");
		assertThat(entity.getSource()).isEqualTo("HTTP");
		assertThat(entity.getAction()).isEqualTo("READ");
		assertThat(entity.getTarget()).isEqualTo("errand");
		assertThat(entity.getDescription()).isEqualTo("READ errand");
		assertThat(entity.getHttpMethod()).isEqualTo("GET");
		assertThat(entity.getRequestPath()).isEqualTo("/2281/FINANCIAL_ASSISTANCE/errands/errand-1");
		assertThat(entity.getActor()).isEqualTo("joe001doe");
		assertThat(entity.getActorType()).isEqualTo("adAccount");
		assertThat(entity.getRequestId()).isEqualTo("req-1");
		assertThat(entity.getStatusCode()).isEqualTo(200);
		assertThat(entity.getCreated()).isEqualTo(FIXED_TIMESTAMP);
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(ErrandEventEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new ErrandEventEntity()).hasAllNullFieldsOrProperties();
	}
}
