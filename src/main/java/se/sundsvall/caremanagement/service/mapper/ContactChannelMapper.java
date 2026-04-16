package se.sundsvall.caremanagement.service.mapper;

import java.util.List;
import se.sundsvall.caremanagement.api.model.ContactChannel;
import se.sundsvall.caremanagement.integration.db.model.TagEmbeddable;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public final class ContactChannelMapper {

	private ContactChannelMapper() {}

	public static ContactChannel toContactChannel(final TagEmbeddable entity) {
		return ofNullable(entity)
			.map(tagEmbeddable -> ContactChannel.create()
				.withKey(tagEmbeddable.getKey())
				.withValue(tagEmbeddable.getValue()))
			.orElse(null);
	}

	public static TagEmbeddable toTagEmbeddable(final ContactChannel channel) {
		return ofNullable(channel)
			.map(contactChannel -> TagEmbeddable.create()
				.withKey(contactChannel.getKey())
				.withValue(contactChannel.getValue()))
			.orElse(null);
	}

	public static List<ContactChannel> toContactChannelList(final List<TagEmbeddable> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(ContactChannelMapper::toContactChannel)
			.toList();
	}

	public static List<TagEmbeddable> toTagEmbeddableList(final List<ContactChannel> channels) {
		return ofNullable(channels).orElse(emptyList()).stream()
			.map(ContactChannelMapper::toTagEmbeddable)
			.toList();
	}
}
