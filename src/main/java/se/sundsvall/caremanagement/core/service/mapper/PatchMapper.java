package se.sundsvall.caremanagement.core.service.mapper;

import se.sundsvall.caremanagement.core.api.model.PatchErrand;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;

import static java.util.Optional.ofNullable;

/**
 * CaseData-style PATCH semantics: {@code null} on a field means leave the entity property untouched.
 * Subcollections (attachments / stakeholders / decisions) are <b>not</b> patched here — they have
 * their own subresources. Type-slug is immutable post-create.
 */
public final class PatchMapper {

	private PatchMapper() {}

	public static ErrandEntity patchErrand(final ErrandEntity entity, final PatchErrand patch) {
		if (entity == null || patch == null) {
			return entity;
		}
		ofNullable(patch.getTitle()).ifPresent(entity::setTitle);
		ofNullable(patch.getStatus()).ifPresent(entity::setStatus);
		ofNullable(patch.getDescription()).ifPresent(entity::setDescription);
		ofNullable(patch.getPriority()).ifPresent(entity::setPriority);
		ofNullable(patch.getReporterUserId()).ifPresent(entity::setReporterUserId);
		ofNullable(patch.getAssignedUserId()).ifPresent(entity::setAssignedUserId);
		return entity;
	}
}
