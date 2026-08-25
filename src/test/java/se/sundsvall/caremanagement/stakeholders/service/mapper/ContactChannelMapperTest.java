package se.sundsvall.caremanagement.stakeholders.service.mapper;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.stakeholders.api.model.ContactChannel;
import se.sundsvall.caremanagement.stakeholders.integration.db.model.TagEmbeddable;

import static org.assertj.core.api.Assertions.assertThat;

class ContactChannelMapperTest {

	@Test
	void toContactChannelMaps() {
		final var result = ContactChannelMapper.toContactChannel(TagEmbeddable.create().withKey("PHONE").withValue("0701234567"));

		assertThat(result).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(result.getKey()).isEqualTo("PHONE");
		assertThat(result.getValue()).isEqualTo("0701234567");
	}

	@Test
	void toContactChannelNullReturnsNull() {
		assertThat(ContactChannelMapper.toContactChannel(null)).isNull();
	}

	@Test
	void toTagEmbeddableMaps() {
		final var result = ContactChannelMapper.toTagEmbeddable(ContactChannel.create().withKey("EMAIL").withValue("a@b.c"));

		assertThat(result).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(result.getKey()).isEqualTo("EMAIL");
		assertThat(result.getValue()).isEqualTo("a@b.c");
	}

	@Test
	void toTagEmbeddableNullReturnsNull() {
		assertThat(ContactChannelMapper.toTagEmbeddable(null)).isNull();
	}

	@Test
	void toContactChannelListMaps() {
		final var result = ContactChannelMapper.toContactChannelList(List.of(
			TagEmbeddable.create().withKey("PHONE").withValue("123"),
			TagEmbeddable.create().withKey("EMAIL").withValue("a@b")));

		assertThat(result).hasSize(2);
	}

	@Test
	void toContactChannelListNullReturnsEmpty() {
		assertThat(ContactChannelMapper.toContactChannelList(null)).isEmpty();
	}

	@Test
	void toTagEmbeddableListMaps() {
		final var result = ContactChannelMapper.toTagEmbeddableList(List.of(
			ContactChannel.create().withKey("PHONE").withValue("123")));

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getKey()).isEqualTo("PHONE");
	}

	@Test
	void toTagEmbeddableListNullReturnsEmpty() {
		assertThat(ContactChannelMapper.toTagEmbeddableList(null)).isEmpty();
	}
}
