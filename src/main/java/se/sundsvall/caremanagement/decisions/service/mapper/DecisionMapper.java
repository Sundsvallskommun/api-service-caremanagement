package se.sundsvall.caremanagement.decisions.service.mapper;

import java.util.List;
import se.sundsvall.caremanagement.decisions.api.model.Decision;
import se.sundsvall.caremanagement.decisions.integration.db.model.DecisionEntity;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public final class DecisionMapper {

	private DecisionMapper() {}

	public static Decision toDecision(final DecisionEntity entity) {
		return ofNullable(entity)
			.map(e -> Decision.create()
				.withId(e.getId())
				.withDecisionType(e.getDecisionType())
				.withValue(e.getValue())
				.withDescription(e.getDescription())
				.withCreatedBy(e.getCreatedBy())
				.withCreated(e.getCreated()))
			.orElse(null);
	}

	public static DecisionEntity toDecisionEntity(final Decision decision, final String errandId) {
		return ofNullable(decision)
			.map(source -> DecisionEntity.create()
				.withErrandId(errandId)
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
}
