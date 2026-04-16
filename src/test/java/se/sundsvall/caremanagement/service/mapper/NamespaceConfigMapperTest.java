package se.sundsvall.caremanagement.service.mapper;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.api.model.NamespaceConfig;
import se.sundsvall.caremanagement.integration.db.model.NamespaceConfigEntity;

import static org.assertj.core.api.Assertions.assertThat;

class NamespaceConfigMapperTest {

	@Test
	void toNamespaceConfig_maps() {
		final var created = OffsetDateTime.now().minusDays(1);
		final var modified = OffsetDateTime.now();
		final var entity = NamespaceConfigEntity.create()
			.withId(42L)
			.withNamespace("ns")
			.withMunicipalityId("2281")
			.withDisplayName("display")
			.withShortCode("sc")
			.withCreated(created)
			.withModified(modified);

		final var result = NamespaceConfigMapper.toNamespaceConfig(entity);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(42L);
		assertThat(result.getDisplayName()).isEqualTo("display");
		assertThat(result.getShortCode()).isEqualTo("sc");
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getModified()).isEqualTo(modified);
	}

	@Test
	void toNamespaceConfig_nullReturnsNull() {
		assertThat(NamespaceConfigMapper.toNamespaceConfig(null)).isNull();
	}

	@Test
	void toNamespaceConfigEntity_maps() {
		final var config = NamespaceConfig.create().withDisplayName("display").withShortCode("sc");

		final var result = NamespaceConfigMapper.toNamespaceConfigEntity(config, "ns", "2281");

		assertThat(result).isNotNull();
		assertThat(result.getNamespace()).isEqualTo("ns");
		assertThat(result.getMunicipalityId()).isEqualTo("2281");
		assertThat(result.getDisplayName()).isEqualTo("display");
		assertThat(result.getShortCode()).isEqualTo("sc");
	}

	@Test
	void toNamespaceConfigEntity_nullConfigReturnsNull() {
		assertThat(NamespaceConfigMapper.toNamespaceConfigEntity(null, "ns", "2281")).isNull();
	}

	@Test
	void updateNamespaceConfigEntity_updates() {
		final var entity = NamespaceConfigEntity.create().withDisplayName("old").withShortCode("o");
		final var source = NamespaceConfig.create().withDisplayName("new").withShortCode("n");

		final var result = NamespaceConfigMapper.updateNamespaceConfigEntity(entity, source);

		assertThat(result).isSameAs(entity);
		assertThat(result.getDisplayName()).isEqualTo("new");
		assertThat(result.getShortCode()).isEqualTo("n");
	}

	@Test
	void updateNamespaceConfigEntity_nullEntity() {
		assertThat(NamespaceConfigMapper.updateNamespaceConfigEntity(null, NamespaceConfig.create())).isNull();
	}

	@Test
	void updateNamespaceConfigEntity_nullSource() {
		final var entity = NamespaceConfigEntity.create().withDisplayName("kept");
		final var result = NamespaceConfigMapper.updateNamespaceConfigEntity(entity, null);
		assertThat(result).isSameAs(entity);
		assertThat(result.getDisplayName()).isEqualTo("kept");
	}
}
