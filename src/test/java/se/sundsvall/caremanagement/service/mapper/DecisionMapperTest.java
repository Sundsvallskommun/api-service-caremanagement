package se.sundsvall.caremanagement.service.mapper;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.api.model.Decision;
import se.sundsvall.caremanagement.integration.db.model.DecisionEntity;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionMapperTest {

	@Test
	void toDecision_maps() {
		final var created = OffsetDateTime.now();
		final var entity = DecisionEntity.create()
			.withId("did")
			.withDecisionType("PAYMENT")
			.withValue("APPROVED")
			.withDescription("desc")
			.withCreatedBy("jane01doe")
			.withCreated(created);

		final var result = DecisionMapper.toDecision(entity);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo("did");
		assertThat(result.getDecisionType()).isEqualTo("PAYMENT");
		assertThat(result.getValue()).isEqualTo("APPROVED");
		assertThat(result.getDescription()).isEqualTo("desc");
		assertThat(result.getCreatedBy()).isEqualTo("jane01doe");
		assertThat(result.getCreated()).isEqualTo(created);
	}

	@Test
	void toDecision_nullEntityReturnsNull() {
		assertThat(DecisionMapper.toDecision(null)).isNull();
	}

	@Test
	void toDecisionEntity_maps() {
		final var errand = ErrandEntity.create().withId("eid");
		final var decision = Decision.create()
			.withDecisionType("RECOMMENDATION")
			.withValue("Beslutsförslag: 7900 kr")
			.withDescription("Inom gränsvärde")
			.withCreatedBy("operaton");

		final var result = DecisionMapper.toDecisionEntity(decision, errand);

		assertThat(result).isNotNull();
		assertThat(result.getErrandEntity()).isSameAs(errand);
		assertThat(result.getDecisionType()).isEqualTo("RECOMMENDATION");
		assertThat(result.getValue()).isEqualTo("Beslutsförslag: 7900 kr");
		assertThat(result.getDescription()).isEqualTo("Inom gränsvärde");
		assertThat(result.getCreatedBy()).isEqualTo("operaton");
		// id and created are server-assigned and intentionally NOT copied from input.
		assertThat(result.getId()).isNull();
		assertThat(result.getCreated()).isNull();
	}

	@Test
	void toDecisionEntity_nullReturnsNull() {
		assertThat(DecisionMapper.toDecisionEntity(null, ErrandEntity.create())).isNull();
	}

	@Test
	void toDecisionList_maps() {
		final var result = DecisionMapper.toDecisionList(List.of(
			DecisionEntity.create().withId("a"),
			DecisionEntity.create().withId("b")));

		assertThat(result).hasSize(2);
	}

	@Test
	void toDecisionList_nullReturnsEmpty() {
		assertThat(DecisionMapper.toDecisionList(null)).isEmpty();
	}

	@Test
	void toDecisionEntityList_maps() {
		final var errand = ErrandEntity.create().withId("eid");
		final var result = DecisionMapper.toDecisionEntityList(List.of(
			Decision.create().withDecisionType("a"),
			Decision.create().withDecisionType("b")), errand);

		assertThat(result).hasSize(2);
		assertThat(result.getFirst().getErrandEntity()).isSameAs(errand);
	}

	@Test
	void toDecisionEntityList_nullReturnsEmpty() {
		assertThat(DecisionMapper.toDecisionEntityList(null, ErrandEntity.create())).isEmpty();
	}
}
