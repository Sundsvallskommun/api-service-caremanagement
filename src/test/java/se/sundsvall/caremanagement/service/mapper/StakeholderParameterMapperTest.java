package se.sundsvall.caremanagement.service.mapper;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.api.model.StakeholderParameter;
import se.sundsvall.caremanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.caremanagement.integration.db.model.StakeholderParameterEntity;

import static org.assertj.core.api.Assertions.assertThat;

class StakeholderParameterMapperTest {

	@Test
	void toStakeholderParameter_maps() {
		final var entity = StakeholderParameterEntity.create()
			.withId(42L)
			.withKey("k")
			.withDisplayName("dn")
			.withValues(new ArrayList<>(List.of("a", "b")));

		final var result = StakeholderParameterMapper.toStakeholderParameter(entity);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(42L);
		assertThat(result.getKey()).isEqualTo("k");
		assertThat(result.getDisplayName()).isEqualTo("dn");
		assertThat(result.getValues()).containsExactly("a", "b");
	}

	@Test
	void toStakeholderParameter_nullValues() {
		final var entity = StakeholderParameterEntity.create().withId(1L).withKey("k");

		final var result = StakeholderParameterMapper.toStakeholderParameter(entity);

		assertThat(result).isNotNull();
		assertThat(result.getValues()).isNull();
	}

	@Test
	void toStakeholderParameter_nullReturnsNull() {
		assertThat(StakeholderParameterMapper.toStakeholderParameter(null)).isNull();
	}

	@Test
	void toStakeholderParameterEntity_maps() {
		final var stakeholder = StakeholderEntity.create().withId("sid");
		final var parameter = StakeholderParameter.create()
			.withKey("k")
			.withDisplayName("dn")
			.withValues(List.of("a"));

		final var result = StakeholderParameterMapper.toStakeholderParameterEntity(parameter, stakeholder);

		assertThat(result).isNotNull();
		assertThat(result.getStakeholderEntity()).isSameAs(stakeholder);
		assertThat(result.getKey()).isEqualTo("k");
		assertThat(result.getDisplayName()).isEqualTo("dn");
		assertThat(result.getValues()).containsExactly("a");
	}

	@Test
	void toStakeholderParameterEntity_nullReturnsNull() {
		assertThat(StakeholderParameterMapper.toStakeholderParameterEntity(null, StakeholderEntity.create())).isNull();
	}

	@Test
	void toStakeholderParameterEntity_nullValues() {
		final var result = StakeholderParameterMapper.toStakeholderParameterEntity(
			StakeholderParameter.create().withKey("k"), StakeholderEntity.create());
		assertThat(result.getValues()).isNull();
	}

	@Test
	void updateStakeholderParameterEntity_updates() {
		final var entity = StakeholderParameterEntity.create().withKey("old").withValues(new ArrayList<>(List.of("a")));
		final var source = StakeholderParameter.create().withKey("new").withDisplayName("dn").withValues(List.of("x"));

		final var result = StakeholderParameterMapper.updateStakeholderParameterEntity(entity, source);

		assertThat(result).isSameAs(entity);
		assertThat(result.getKey()).isEqualTo("new");
		assertThat(result.getDisplayName()).isEqualTo("dn");
		assertThat(result.getValues()).containsExactly("x");
	}

	@Test
	void updateStakeholderParameterEntity_nullSourceValuesPreservesExisting() {
		final var entity = StakeholderParameterEntity.create().withValues(new ArrayList<>(List.of("a")));
		final var source = StakeholderParameter.create().withKey("k");

		final var result = StakeholderParameterMapper.updateStakeholderParameterEntity(entity, source);

		assertThat(result.getKey()).isEqualTo("k");
		assertThat(result.getValues()).containsExactly("a");
	}

	@Test
	void updateStakeholderParameterEntity_nullEntity() {
		assertThat(StakeholderParameterMapper.updateStakeholderParameterEntity(null, StakeholderParameter.create())).isNull();
	}

	@Test
	void updateStakeholderParameterEntity_nullSource() {
		final var entity = StakeholderParameterEntity.create().withKey("kept");
		final var result = StakeholderParameterMapper.updateStakeholderParameterEntity(entity, null);
		assertThat(result).isSameAs(entity);
		assertThat(result.getKey()).isEqualTo("kept");
	}

	@Test
	void toStakeholderParameterList_maps() {
		final var result = StakeholderParameterMapper.toStakeholderParameterList(List.of(
			StakeholderParameterEntity.create().withId(1L),
			StakeholderParameterEntity.create().withId(2L)));

		assertThat(result).hasSize(2);
	}

	@Test
	void toStakeholderParameterList_nullReturnsEmpty() {
		assertThat(StakeholderParameterMapper.toStakeholderParameterList(null)).isEmpty();
	}

	@Test
	void toStakeholderParameterEntityList_maps() {
		final var stakeholder = StakeholderEntity.create().withId("sid");
		final var result = StakeholderParameterMapper.toStakeholderParameterEntityList(List.of(
			StakeholderParameter.create().withKey("a"),
			StakeholderParameter.create().withKey("b")), stakeholder);

		assertThat(result).hasSize(2);
		assertThat(result.getFirst().getStakeholderEntity()).isSameAs(stakeholder);
	}

	@Test
	void toStakeholderParameterEntityList_nullReturnsEmpty() {
		assertThat(StakeholderParameterMapper.toStakeholderParameterEntityList(null, StakeholderEntity.create())).isEmpty();
	}
}
