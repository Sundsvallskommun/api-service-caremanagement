package se.sundsvall.caremanagement.service.mapper;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.api.model.Lookup;
import se.sundsvall.caremanagement.integration.db.model.LookupEntity;
import se.sundsvall.caremanagement.integration.db.model.LookupKind;

import static org.assertj.core.api.Assertions.assertThat;

class LookupMapperTest {

	@Test
	void toLookup_maps() {
		final var created = OffsetDateTime.now().minusDays(1);
		final var modified = OffsetDateTime.now();
		final var entity = LookupEntity.create()
			.withKind(LookupKind.STATUS)
			.withName("NEW")
			.withDisplayName("New case")
			.withNamespace("ns")
			.withMunicipalityId("2281")
			.withCreated(created)
			.withModified(modified);

		final var result = LookupMapper.toLookup(entity);

		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo("NEW");
		assertThat(result.getDisplayName()).isEqualTo("New case");
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getModified()).isEqualTo(modified);
	}

	@Test
	void toLookup_nullReturnsNull() {
		assertThat(LookupMapper.toLookup(null)).isNull();
	}

	@Test
	void toLookupEntity_maps() {
		final var lookup = Lookup.create().withName("NEW").withDisplayName("New case");

		final var result = LookupMapper.toLookupEntity(lookup, LookupKind.STATUS, "ns", "2281");

		assertThat(result).isNotNull();
		assertThat(result.getKind()).isEqualTo(LookupKind.STATUS);
		assertThat(result.getName()).isEqualTo("NEW");
		assertThat(result.getDisplayName()).isEqualTo("New case");
		assertThat(result.getNamespace()).isEqualTo("ns");
		assertThat(result.getMunicipalityId()).isEqualTo("2281");
	}

	@Test
	void toLookupEntity_nullLookupReturnsNull() {
		assertThat(LookupMapper.toLookupEntity(null, LookupKind.STATUS, "ns", "2281")).isNull();
	}

	@Test
	void updateLookupEntity_updatesDisplayNameOnly() {
		final var entity = LookupEntity.create().withName("NEW").withDisplayName("Old").withKind(LookupKind.STATUS);
		final var source = Lookup.create().withName("IGNORED").withDisplayName("Updated");

		final var result = LookupMapper.updateLookupEntity(entity, source);

		assertThat(result).isSameAs(entity);
		assertThat(result.getName()).isEqualTo("NEW");
		assertThat(result.getDisplayName()).isEqualTo("Updated");
	}

	@Test
	void updateLookupEntity_nullEntityReturnsNull() {
		assertThat(LookupMapper.updateLookupEntity(null, Lookup.create())).isNull();
	}

	@Test
	void updateLookupEntity_nullSourceReturnsEntity() {
		final var entity = LookupEntity.create().withDisplayName("kept");

		final var result = LookupMapper.updateLookupEntity(entity, null);

		assertThat(result).isSameAs(entity);
		assertThat(result.getDisplayName()).isEqualTo("kept");
	}

	@Test
	void toLookupList_maps() {
		final var result = LookupMapper.toLookupList(List.of(
			LookupEntity.create().withName("A"),
			LookupEntity.create().withName("B")));

		assertThat(result).hasSize(2);
	}

	@Test
	void toLookupList_nullReturnsEmpty() {
		assertThat(LookupMapper.toLookupList(null)).isEmpty();
	}
}
