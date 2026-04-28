package se.sundsvall.caremanagement.service.mapper;

import java.util.List;
import se.sundsvall.caremanagement.api.model.Decision;
import se.sundsvall.caremanagement.integration.db.model.DecisionEntity;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public final class DecisionMapper {

	private DecisionMapper() {}

	public static Decision toDecision(final DecisionEntity entity) {
		return ofNullable(entity)
			.map(decisionEntity -> Decision.create()
				.withId(decisionEntity.getId())
				.withDecisionType(decisionEntity.getDecisionType())
				.withValue(decisionEntity.getValue())
				.withDescription(decisionEntity.getDescription())
				.withCreatedBy(decisionEntity.getCreatedBy())
				.withCreated(decisionEntity.getCreated()))
			.orElse(null);
	}

	public static DecisionEntity toDecisionEntity(final Decision decision, final ErrandEntity errandEntity) {
		return ofNullable(decision)
			.map(source -> DecisionEntity.create()
				.withErrandEntity(errandEntity)
				.withDecisionType(source.getDecisionType())
				.withValue(source.getValue())
				.withDescription(source.getDescription())
				.withCreatedBy(source.getCreatedBy()))
			.orElse(null);
	}

	public static List<Decision> toDecisionList(final List<DecisionEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(DecisionMapper::toDecision)
			.toList();
	}

	public static List<DecisionEntity> toDecisionEntityList(final List<Decision> decisions, final ErrandEntity errandEntity) {
		return ofNullable(decisions).orElse(emptyList()).stream()
			.map(decision -> toDecisionEntity(decision, errandEntity))
			.toList();
	}
}
