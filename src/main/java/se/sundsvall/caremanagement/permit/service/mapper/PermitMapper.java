package se.sundsvall.caremanagement.permit.service.mapper;

import java.util.List;
import se.sundsvall.caremanagement.permit.api.model.Permit;
import se.sundsvall.caremanagement.permit.integration.db.model.PermitEntity;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public final class PermitMapper {

	private PermitMapper() {}

	public static Permit toPermit(final PermitEntity entity) {
		return ofNullable(entity)
			.map(e -> Permit.create()
				.withId(e.getId())
				.withPermitType(e.getPermitType())
				.withValidFrom(e.getValidFrom())
				.withValidUntil(e.getValidUntil())
				.withConditions(e.getConditions())
				.withStatus(e.getStatus())
				.withCreated(e.getCreated())
				.withModified(e.getModified()))
			.orElse(null);
	}

	public static PermitEntity toPermitEntity(final Permit permit, final String errandId) {
		return ofNullable(permit)
			.map(source -> PermitEntity.create()
				.withErrandId(errandId)
				.withPermitType(source.getPermitType())
				.withValidFrom(source.getValidFrom())
				.withValidUntil(source.getValidUntil())
				.withConditions(source.getConditions())
				.withStatus(source.getStatus()))
			.orElse(null);
	}

	public static List<Permit> toPermitList(final List<PermitEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(PermitMapper::toPermit)
			.toList();
	}
}
