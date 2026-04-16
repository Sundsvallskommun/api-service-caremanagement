package se.sundsvall.caremanagement.service.mapper;

import java.util.ArrayList;
import se.sundsvall.caremanagement.api.model.PatchErrand;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.integration.db.model.LookupEntity;

import static java.util.Optional.ofNullable;
import static se.sundsvall.caremanagement.service.mapper.ExternalTagMapper.toTagEmbeddableList;

/**
 * Casedata-style PATCH semantics: {@code null} on a field means leave the entity property untouched. Subcollections
 * (attachments / stakeholders / parameters) are <b>not</b> patched through {@link PatchErrand} — they have their own
 * subresources.
 */
public final class PatchMapper {

	private PatchMapper() {}

	/**
	 * Applies non-null fields from {@code patch} onto {@code entity}. {@code contactReason} resolution (string name
	 * → {@link LookupEntity}) is the caller's responsibility; pass {@code null} to leave unchanged.
	 */
	public static ErrandEntity patchErrand(final ErrandEntity entity, final PatchErrand patch, final LookupEntity resolvedContactReason) {
		if (entity == null || patch == null) {
			return entity;
		}
		ofNullable(patch.getTitle()).ifPresent(entity::setTitle);
		ofNullable(patch.getCategory()).ifPresent(entity::setCategory);
		ofNullable(patch.getType()).ifPresent(entity::setType);
		ofNullable(patch.getStatus()).ifPresent(entity::setStatus);
		ofNullable(patch.getDescription()).ifPresent(entity::setDescription);
		ofNullable(patch.getPriority()).ifPresent(entity::setPriority);
		ofNullable(patch.getReporterUserId()).ifPresent(entity::setReporterUserId);
		ofNullable(patch.getAssignedUserId()).ifPresent(entity::setAssignedUserId);
		ofNullable(patch.getContactReasonDescription()).ifPresent(entity::setContactReasonDescription);
		ofNullable(patch.getExternalTags()).ifPresent(tags -> entity.setExternalTags(new ArrayList<>(toTagEmbeddableList(tags))));
		ofNullable(patch.getContactReason()).ifPresent(_ -> entity.setContactReason(resolvedContactReason));
		return entity;
	}
}
