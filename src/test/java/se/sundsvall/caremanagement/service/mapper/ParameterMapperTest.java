package se.sundsvall.caremanagement.service.mapper;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.api.model.Parameter;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.integration.db.model.ParameterEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ParameterMapperTest {

	@Test
	void toParameter_maps() {
		final var values = new ArrayList<>(List.of("a", "b"));
		final var entity = ParameterEntity.create()
			.withId("pid")
			.withKey("k")
			.withDisplayName("dn")
			.withParameterGroup("g")
			.withValues(values);

		final var result = ParameterMapper.toParameter(entity);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo("pid");
		assertThat(result.getKey()).isEqualTo("k");
		assertThat(result.getDisplayName()).isEqualTo("dn");
		assertThat(result.getParameterGroup()).isEqualTo("g");
		assertThat(result.getValues()).containsExactly("a", "b");
		assertThat(result.getValues()).isNotSameAs(values);
	}

	@Test
	void toParameter_nullValues() {
		final var entity = ParameterEntity.create().withId("pid").withKey("k");

		final var result = ParameterMapper.toParameter(entity);

		assertThat(result).isNotNull();
		assertThat(result.getValues()).isNull();
	}

	@Test
	void toParameter_nullEntityReturnsNull() {
		assertThat(ParameterMapper.toParameter(null)).isNull();
	}

	@Test
	void toParameterEntity_maps() {
		final var errand = ErrandEntity.create().withId("eid");
		final var parameter = Parameter.create()
			.withKey("k")
			.withDisplayName("dn")
			.withParameterGroup("g")
			.withValues(List.of("a", "b"));

		final var result = ParameterMapper.toParameterEntity(parameter, errand);

		assertThat(result).isNotNull();
		assertThat(result.getErrandEntity()).isSameAs(errand);
		assertThat(result.getKey()).isEqualTo("k");
		assertThat(result.getDisplayName()).isEqualTo("dn");
		assertThat(result.getParameterGroup()).isEqualTo("g");
		assertThat(result.getValues()).containsExactly("a", "b");
	}

	@Test
	void toParameterEntity_nullReturnsNull() {
		assertThat(ParameterMapper.toParameterEntity(null, ErrandEntity.create())).isNull();
	}

	@Test
	void toParameterEntity_nullValues() {
		final var result = ParameterMapper.toParameterEntity(Parameter.create().withKey("k"), ErrandEntity.create());
		assertThat(result.getValues()).isNull();
	}

	@Test
	void updateParameterEntity_updates() {
		final var entity = ParameterEntity.create().withKey("old");
		final var source = Parameter.create()
			.withKey("new")
			.withDisplayName("dn")
			.withParameterGroup("g")
			.withValues(List.of("x"));

		final var result = ParameterMapper.updateParameterEntity(entity, source);

		assertThat(result).isSameAs(entity);
		assertThat(result.getKey()).isEqualTo("new");
		assertThat(result.getDisplayName()).isEqualTo("dn");
		assertThat(result.getParameterGroup()).isEqualTo("g");
		assertThat(result.getValues()).containsExactly("x");
	}

	@Test
	void updateParameterEntity_nullValues() {
		final var entity = ParameterEntity.create().withKey("old").withValues(new ArrayList<>(List.of("a")));
		final var source = Parameter.create().withKey("new");

		final var result = ParameterMapper.updateParameterEntity(entity, source);

		assertThat(result.getValues()).isNull();
	}

	@Test
	void updateParameterEntity_nullEntity() {
		assertThat(ParameterMapper.updateParameterEntity(null, Parameter.create())).isNull();
	}

	@Test
	void updateParameterEntity_nullSource() {
		final var entity = ParameterEntity.create().withKey("kept");
		final var result = ParameterMapper.updateParameterEntity(entity, null);
		assertThat(result).isSameAs(entity);
		assertThat(result.getKey()).isEqualTo("kept");
	}

	@Test
	void toParameterList_maps() {
		final var result = ParameterMapper.toParameterList(List.of(
			ParameterEntity.create().withId("a"),
			ParameterEntity.create().withId("b")));

		assertThat(result).hasSize(2);
	}

	@Test
	void toParameterList_nullReturnsEmpty() {
		assertThat(ParameterMapper.toParameterList(null)).isEmpty();
	}

	@Test
	void toParameterEntityList_maps() {
		final var errand = ErrandEntity.create().withId("eid");
		final var result = ParameterMapper.toParameterEntityList(List.of(
			Parameter.create().withKey("a"),
			Parameter.create().withKey("b")), errand);

		assertThat(result).hasSize(2);
		assertThat(result.getFirst().getErrandEntity()).isSameAs(errand);
	}

	@Test
	void toParameterEntityList_nullReturnsEmpty() {
		assertThat(ParameterMapper.toParameterEntityList(null, ErrandEntity.create())).isEmpty();
	}
}
