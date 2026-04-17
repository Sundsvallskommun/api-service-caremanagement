package se.sundsvall.caremanagement.service.mapper;

import java.util.List;
import se.sundsvall.caremanagement.api.model.ExternalTag;
import se.sundsvall.caremanagement.integration.db.model.TagEmbeddable;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public final class ExternalTagMapper {

	private ExternalTagMapper() {}

	public static ExternalTag toExternalTag(final TagEmbeddable entity) {
		return ofNullable(entity)
			.map(tagEmbeddable -> ExternalTag.create()
				.withKey(tagEmbeddable.getKey())
				.withValue(tagEmbeddable.getValue()))
			.orElse(null);
	}

	public static TagEmbeddable toTagEmbeddable(final ExternalTag tag) {
		return ofNullable(tag)
			.map(externalTag -> TagEmbeddable.create()
				.withKey(externalTag.getKey())
				.withValue(externalTag.getValue()))
			.orElse(null);
	}

	public static List<ExternalTag> toExternalTagList(final List<TagEmbeddable> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(ExternalTagMapper::toExternalTag)
			.toList();
	}

	public static List<TagEmbeddable> toTagEmbeddableList(final List<ExternalTag> tags) {
		return ofNullable(tags).orElse(emptyList()).stream()
			.map(ExternalTagMapper::toTagEmbeddable)
			.toList();
	}
}
