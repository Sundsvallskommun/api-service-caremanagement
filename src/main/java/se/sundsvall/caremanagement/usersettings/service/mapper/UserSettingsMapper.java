package se.sundsvall.caremanagement.usersettings.service.mapper;

import se.sundsvall.caremanagement.usersettings.api.model.UserSettings;
import se.sundsvall.caremanagement.usersettings.integration.db.model.UserSettingsEntity;

import static java.util.Optional.ofNullable;

public final class UserSettingsMapper {

	public static final boolean DEFAULT_SSBTEK_OPEN_IN_NEW_WINDOW = true;

	private UserSettingsMapper() {}

	public static UserSettings toUserSettings(final UserSettingsEntity entity) {
		return ofNullable(entity)
			.map(settingsEntity -> UserSettings.create()
				.withAdAccount(settingsEntity.getAdAccount())
				.withSsbtekOpenInNewWindow(settingsEntity.getSsbtekOpenInNewWindow())
				.withCreated(settingsEntity.getCreated())
				.withModified(settingsEntity.getModified()))
			.orElse(null);
	}

	/**
	 * The settings a user gets before saving any: nothing stored, so no timestamps.
	 */
	public static UserSettings toDefaultUserSettings(final String adAccount) {
		return UserSettings.create()
			.withAdAccount(adAccount)
			.withSsbtekOpenInNewWindow(DEFAULT_SSBTEK_OPEN_IN_NEW_WINDOW);
	}

	public static UserSettingsEntity toUserSettingsEntity(final UserSettings settings, final String municipalityId, final String adAccount) {
		return ofNullable(settings)
			.map(source -> UserSettingsEntity.create()
				.withMunicipalityId(municipalityId)
				.withAdAccount(adAccount)
				.withSsbtekOpenInNewWindow(source.getSsbtekOpenInNewWindow()))
			.orElse(null);
	}

	public static UserSettingsEntity updateUserSettingsEntity(final UserSettingsEntity entity, final UserSettings source) {
		if (entity == null || source == null) {
			return entity;
		}
		entity.setSsbtekOpenInNewWindow(source.getSsbtekOpenInNewWindow());
		return entity;
	}
}
