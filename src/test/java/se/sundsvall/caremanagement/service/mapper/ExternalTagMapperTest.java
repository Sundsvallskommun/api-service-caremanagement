package se.sundsvall.caremanagement.service.mapper;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.api.model.ExternalTag;
import se.sundsvall.caremanagement.integration.db.model.TagEmbeddable;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalTagMapperTest {

	@Test
	void toExternalTag_maps() {
		final var result = ExternalTagMapper.toExternalTag(TagEmbeddable.create().withKey("k").withValue("v"));

		assertThat(result).isNotNull();
		assertThat(result.getKey()).isEqualTo("k");
		assertThat(result.getValue()).isEqualTo("v");
	}

	@Test
	void toExternalTag_nullReturnsNull() {
		assertThat(ExternalTagMapper.toExternalTag(null)).isNull();
	}

	@Test
	void toTagEmbeddable_maps() {
		final var result = ExternalTagMapper.toTagEmbeddable(ExternalTag.create().withKey("k").withValue("v"));

		assertThat(result).isNotNull();
		assertThat(result.getKey()).isEqualTo("k");
		assertThat(result.getValue()).isEqualTo("v");
	}

	@Test
	void toTagEmbeddable_nullReturnsNull() {
		assertThat(ExternalTagMapper.toTagEmbeddable(null)).isNull();
	}

	@Test
	void toExternalTagList_maps() {
		final var result = ExternalTagMapper.toExternalTagList(List.of(
			TagEmbeddable.create().withKey("k1").withValue("v1"),
			TagEmbeddable.create().withKey("k2").withValue("v2")));

		assertThat(result).hasSize(2);
		assertThat(result.getFirst().getKey()).isEqualTo("k1");
		assertThat(result.get(1).getValue()).isEqualTo("v2");
	}

	@Test
	void toExternalTagList_nullReturnsEmpty() {
		assertThat(ExternalTagMapper.toExternalTagList(null)).isEmpty();
	}

	@Test
	void toTagEmbeddableList_maps() {
		final var result = ExternalTagMapper.toTagEmbeddableList(List.of(
			ExternalTag.create().withKey("k1").withValue("v1"),
			ExternalTag.create().withKey("k2").withValue("v2")));

		assertThat(result).hasSize(2);
		assertThat(result.getFirst().getKey()).isEqualTo("k1");
		assertThat(result.get(1).getValue()).isEqualTo("v2");
	}

	@Test
	void toTagEmbeddableList_nullReturnsEmpty() {
		assertThat(ExternalTagMapper.toTagEmbeddableList(null)).isEmpty();
	}
}
