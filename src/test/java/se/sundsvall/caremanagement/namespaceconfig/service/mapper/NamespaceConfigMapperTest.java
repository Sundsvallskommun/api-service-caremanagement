package se.sundsvall.caremanagement.namespaceconfig.service.mapper;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.namespaceconfig.api.model.NamespaceConfig;
import se.sundsvall.caremanagement.namespaceconfig.integration.db.model.NamespaceConfigEntity;

import static org.assertj.core.api.Assertions.assertThat;

class NamespaceConfigMapperTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@Test
	void toNamespaceConfigMaps() {
		final var created = FIXED_TIMESTAMP.minusDays(1);
		final var modified = FIXED_TIMESTAMP;
		final var entity = NamespaceConfigEntity.create()
			.withId(42L)
			.withNamespace("ns")
			.withMunicipalityId("2281")
			.withDisplayName("display")
			.withShortCode("sc")
			.withCreated(created)
			.withModified(modified);

		final var result = NamespaceConfigMapper.toNamespaceConfig(entity);

		assertThat(result).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(42L);
		assertThat(result.getDisplayName()).isEqualTo("display");
		assertThat(result.getShortCode()).isEqualTo("sc");
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getModified()).isEqualTo(modified);
	}

	@Test
	void toNamespaceConfigNullReturnsNull() {
		assertThat(NamespaceConfigMapper.toNamespaceConfig(null)).isNull();
	}

	@Test
	void toNamespaceConfigEntityMaps() {
		final var config = NamespaceConfig.create().withDisplayName("display").withShortCode("sc");

		final var result = NamespaceConfigMapper.toNamespaceConfigEntity(config, "ns", "2281");

		assertThat(result).isNotNull().hasNoNullFieldsOrPropertiesExcept("id", "created", "modified");
		assertThat(result.getNamespace()).isEqualTo("ns");
		assertThat(result.getMunicipalityId()).isEqualTo("2281");
		assertThat(result.getDisplayName()).isEqualTo("display");
		assertThat(result.getShortCode()).isEqualTo("sc");
	}

	@Test
	void toNamespaceConfigEntityNullConfigReturnsNull() {
		assertThat(NamespaceConfigMapper.toNamespaceConfigEntity(null, "ns", "2281")).isNull();
	}

	@Test
	void updateNamespaceConfigEntityUpdates() {
		final var entity = NamespaceConfigEntity.create().withDisplayName("old").withShortCode("o");
		final var source = NamespaceConfig.create().withDisplayName("new").withShortCode("n");

		final var result = NamespaceConfigMapper.updateNamespaceConfigEntity(entity, source);

		assertThat(result).isSameAs(entity);
		assertThat(result.getDisplayName()).isEqualTo("new");
		assertThat(result.getShortCode()).isEqualTo("n");
	}

	@Test
	void updateNamespaceConfigEntityNullEntity() {
		assertThat(NamespaceConfigMapper.updateNamespaceConfigEntity(null, NamespaceConfig.create())).isNull();
	}

	@Test
	void updateNamespaceConfigEntityNullSource() {
		final var entity = NamespaceConfigEntity.create().withDisplayName("kept");
		final var result = NamespaceConfigMapper.updateNamespaceConfigEntity(entity, null);
		assertThat(result).isSameAs(entity);
		assertThat(result.getDisplayName()).isEqualTo("kept");
	}
}
