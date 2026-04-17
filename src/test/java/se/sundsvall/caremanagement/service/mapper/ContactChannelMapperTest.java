package se.sundsvall.caremanagement.service.mapper;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.api.model.ContactChannel;
import se.sundsvall.caremanagement.integration.db.model.TagEmbeddable;

import static org.assertj.core.api.Assertions.assertThat;

class ContactChannelMapperTest {

	@Test
	void toContactChannel_maps() {
		final var result = ContactChannelMapper.toContactChannel(TagEmbeddable.create().withKey("PHONE").withValue("0701234567"));

		assertThat(result).isNotNull();
		assertThat(result.getKey()).isEqualTo("PHONE");
		assertThat(result.getValue()).isEqualTo("0701234567");
	}

	@Test
	void toContactChannel_nullReturnsNull() {
		assertThat(ContactChannelMapper.toContactChannel(null)).isNull();
	}

	@Test
	void toTagEmbeddable_maps() {
		final var result = ContactChannelMapper.toTagEmbeddable(ContactChannel.create().withKey("EMAIL").withValue("a@b.c"));

		assertThat(result).isNotNull();
		assertThat(result.getKey()).isEqualTo("EMAIL");
		assertThat(result.getValue()).isEqualTo("a@b.c");
	}

	@Test
	void toTagEmbeddable_nullReturnsNull() {
		assertThat(ContactChannelMapper.toTagEmbeddable(null)).isNull();
	}

	@Test
	void toContactChannelList_maps() {
		final var result = ContactChannelMapper.toContactChannelList(List.of(
			TagEmbeddable.create().withKey("PHONE").withValue("123"),
			TagEmbeddable.create().withKey("EMAIL").withValue("a@b")));

		assertThat(result).hasSize(2);
	}

	@Test
	void toContactChannelList_nullReturnsEmpty() {
		assertThat(ContactChannelMapper.toContactChannelList(null)).isEmpty();
	}

	@Test
	void toTagEmbeddableList_maps() {
		final var result = ContactChannelMapper.toTagEmbeddableList(List.of(
			ContactChannel.create().withKey("PHONE").withValue("123")));

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getKey()).isEqualTo("PHONE");
	}

	@Test
	void toTagEmbeddableList_nullReturnsEmpty() {
		assertThat(ContactChannelMapper.toTagEmbeddableList(null)).isEmpty();
	}
}
