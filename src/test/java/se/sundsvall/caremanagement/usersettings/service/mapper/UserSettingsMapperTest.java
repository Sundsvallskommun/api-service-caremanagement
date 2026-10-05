package se.sundsvall.caremanagement.usersettings.service.mapper;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.usersettings.api.model.UserSettings;
import se.sundsvall.caremanagement.usersettings.integration.db.model.UserSettingsEntity;

import static org.assertj.core.api.Assertions.assertThat;

class UserSettingsMapperTest {

	private static final OffsetDateTime CREATED = OffsetDateTime.parse("2026-09-24T10:00:00Z");
	private static final OffsetDateTime MODIFIED = OffsetDateTime.parse("2026-09-24T11:00:00Z");

	@Test
	void toUserSettings() {
		final var entity = UserSettingsEntity.create()
			.withId(1L)
			.withMunicipalityId("2281")
			.withAdAccount("joe01doe")
			.withSsbtekOpenInNewWindow(false)
			.withCreated(CREATED)
			.withModified(MODIFIED);

		assertThat(UserSettingsMapper.toUserSettings(entity)).isEqualTo(UserSettings.create()
			.withAdAccount("joe01doe")
			.withSsbtekOpenInNewWindow(false)
			.withCreated(CREATED)
			.withModified(MODIFIED));
	}

	@Test
	void toUserSettingsFromNull() {
		assertThat(UserSettingsMapper.toUserSettings(null)).isNull();
	}

	@Test
	void toDefaultUserSettings() {
		assertThat(UserSettingsMapper.toDefaultUserSettings("joe01doe")).isEqualTo(UserSettings.create()
			.withAdAccount("joe01doe")
			.withSsbtekOpenInNewWindow(true));
	}

	@Test
	void toUserSettingsEntity() {
		final var result = UserSettingsMapper.toUserSettingsEntity(UserSettings.create().withSsbtekOpenInNewWindow(false), "2281", "joe01doe");

		assertThat(result).isEqualTo(UserSettingsEntity.create()
			.withMunicipalityId("2281")
			.withAdAccount("joe01doe")
			.withSsbtekOpenInNewWindow(false));
	}

	@Test
	void toUserSettingsEntityFromNull() {
		assertThat(UserSettingsMapper.toUserSettingsEntity(null, "2281", "joe01doe")).isNull();
	}

	@Test
	void updateUserSettingsEntity() {
		final var entity = UserSettingsEntity.create().withId(1L).withSsbtekOpenInNewWindow(true);

		final var result = UserSettingsMapper.updateUserSettingsEntity(entity, UserSettings.create().withSsbtekOpenInNewWindow(false));

		assertThat(result).isSameAs(entity);
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getSsbtekOpenInNewWindow()).isFalse();
	}

	@Test
	void updateUserSettingsEntityWithNulls() {
		final var entity = UserSettingsEntity.create().withSsbtekOpenInNewWindow(true);

		assertThat(UserSettingsMapper.updateUserSettingsEntity(null, UserSettings.create())).isNull();
		assertThat(UserSettingsMapper.updateUserSettingsEntity(entity, null)).isSameAs(entity);
		assertThat(entity.getSsbtekOpenInNewWindow()).isTrue();
	}
}
