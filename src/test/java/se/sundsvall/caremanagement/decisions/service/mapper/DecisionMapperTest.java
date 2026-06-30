package se.sundsvall.caremanagement.decisions.service.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.decisions.api.model.Decision;
import se.sundsvall.caremanagement.decisions.integration.db.model.DecisionEntity;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionMapperTest {

	private static final String ID = "cb20c51f-fcf3-42c0-b613-de563634a8ec";
	private static final String ERRAND_ID = "e1d2c3b4-a5f6-7890-1234-567890abcdef";
	private static final String DECISION_TYPE = "PAYMENT";
	private static final String VALUE = "APPROVED";
	private static final String DESCRIPTION = "Decision proposal per ruleset: 7900 kr, no warning";
	private static final BigDecimal AMOUNT = new BigDecimal("7900.00");
	private static final String DECISION_MESSAGE = "Du beviljas financial assistance for juni 2026 enligt riksnorm.";
	private static final LocalDate DECISION_DATE = LocalDate.parse("2026-06-18");
	private static final LocalDate PERIOD_FROM = LocalDate.parse("2026-06-01");
	private static final LocalDate PERIOD_TO = LocalDate.parse("2026-06-30");
	private static final String CREATED_BY = "jane01doe";
	private static final OffsetDateTime CREATED = OffsetDateTime.parse("2026-06-18T09:15:30Z");

	@Test
	void toDecisionMapsEveryField() {
		final var entity = DecisionEntity.create()
			.withId(ID)
			.withErrandId(ERRAND_ID)
			.withDecisionType(DECISION_TYPE)
			.withValue(VALUE)
			.withDescription(DESCRIPTION)
			.withAmount(AMOUNT)
			.withDecisionMessage(DECISION_MESSAGE)
			.withDecisionDate(DECISION_DATE)
			.withPeriodFrom(PERIOD_FROM)
			.withPeriodTo(PERIOD_TO)
			.withCreatedBy(CREATED_BY)
			.withCreated(CREATED);

		final var decision = DecisionMapper.toDecision(entity);

		assertThat(decision).isNotNull();
		assertThat(decision.getId()).isEqualTo(ID);
		assertThat(decision.getDecisionType()).isEqualTo(DECISION_TYPE);
		assertThat(decision.getValue()).isEqualTo(VALUE);
		assertThat(decision.getDescription()).isEqualTo(DESCRIPTION);
		assertThat(decision.getAmount()).isEqualByComparingTo(AMOUNT);
		assertThat(decision.getDecisionMessage()).isEqualTo(DECISION_MESSAGE);
		assertThat(decision.getDecisionDate()).isEqualTo(DECISION_DATE);
		assertThat(decision.getPeriodFrom()).isEqualTo(PERIOD_FROM);
		assertThat(decision.getPeriodTo()).isEqualTo(PERIOD_TO);
		assertThat(decision.getCreatedBy()).isEqualTo(CREATED_BY);
		assertThat(decision.getCreated()).isEqualTo(CREATED);
	}

	@Test
	void toDecisionNullReturnsNull() {
		assertThat(DecisionMapper.toDecision(null)).isNull();
	}

	@Test
	void toDecisionEntityMapsEveryField() {
		final var decision = Decision.create()
			.withId(ID)
			.withDecisionType(DECISION_TYPE)
			.withValue(VALUE)
			.withDescription(DESCRIPTION)
			.withAmount(AMOUNT)
			.withDecisionMessage(DECISION_MESSAGE)
			.withDecisionDate(DECISION_DATE)
			.withPeriodFrom(PERIOD_FROM)
			.withPeriodTo(PERIOD_TO)
			.withCreatedBy(CREATED_BY)
			.withCreated(CREATED);

		final var entity = DecisionMapper.toDecisionEntity(decision, ERRAND_ID);

		assertThat(entity).isNotNull();
		// errandId comes from the argument, not the source DTO
		assertThat(entity.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(entity.getDecisionType()).isEqualTo(DECISION_TYPE);
		assertThat(entity.getValue()).isEqualTo(VALUE);
		assertThat(entity.getDescription()).isEqualTo(DESCRIPTION);
		assertThat(entity.getAmount()).isEqualByComparingTo(AMOUNT);
		assertThat(entity.getDecisionMessage()).isEqualTo(DECISION_MESSAGE);
		assertThat(entity.getDecisionDate()).isEqualTo(DECISION_DATE);
		assertThat(entity.getPeriodFrom()).isEqualTo(PERIOD_FROM);
		assertThat(entity.getPeriodTo()).isEqualTo(PERIOD_TO);
		assertThat(entity.getCreatedBy()).isEqualTo(CREATED_BY);
		// id and created are server/JPA-assigned and must not be carried over from the DTO
		assertThat(entity.getId()).isNull();
		assertThat(entity.getCreated()).isNull();
	}

	@Test
	void toDecisionEntityNullReturnsNull() {
		assertThat(DecisionMapper.toDecisionEntity(null, ERRAND_ID)).isNull();
	}

	@Test
	void toDecisionListMapsEveryItem() {
		final var first = DecisionEntity.create().withId(ID).withDecisionType("RECOMMENDATION").withValue("APPROVED");
		final var second = DecisionEntity.create().withId("00000000-0000-0000-0000-000000000002").withDecisionType("PAYMENT").withValue("REJECTED");

		final var result = DecisionMapper.toDecisionList(List.of(first, second));

		assertThat(result).hasSize(2);
		assertThat(result.get(0).getId()).isEqualTo(ID);
		assertThat(result.get(0).getDecisionType()).isEqualTo("RECOMMENDATION");
		assertThat(result.get(1).getId()).isEqualTo("00000000-0000-0000-0000-000000000002");
		assertThat(result.get(1).getValue()).isEqualTo("REJECTED");
	}

	@Test
	void toDecisionListNullReturnsEmpty() {
		assertThat(DecisionMapper.toDecisionList(null)).isEmpty();
	}
}
