package se.sundsvall.caremanagement.usersettings.service;

import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.usersettings.api.model.UserSettings;
import se.sundsvall.caremanagement.usersettings.integration.db.UserSettingsRepository;
import se.sundsvall.caremanagement.usersettings.service.mapper.UserSettingsMapper;

import static se.sundsvall.caremanagement.usersettings.service.mapper.UserSettingsMapper.toDefaultUserSettings;
import static se.sundsvall.caremanagement.usersettings.service.mapper.UserSettingsMapper.toUserSettingsEntity;
import static se.sundsvall.caremanagement.usersettings.service.mapper.UserSettingsMapper.updateUserSettingsEntity;

/**
 * AD accounts are case-insensitive, so they are stored and looked up in lower case — {@code JOE01DOE} and
 * {@code joe01doe} are the same user.
 */
@Service
@Transactional
public class UserSettingsService {

	private final UserSettingsRepository userSettingsRepository;

	UserSettingsService(final UserSettingsRepository userSettingsRepository) {
		this.userSettingsRepository = userSettingsRepository;
	}

	public UserSettings read(final String municipalityId, final String adAccount) {
		final var normalized = normalize(adAccount);
		return userSettingsRepository.findByMunicipalityIdAndAdAccount(municipalityId, normalized)
			.map(UserSettingsMapper::toUserSettings)
			.orElseGet(() -> toDefaultUserSettings(normalized));
	}

	/**
	 * Creates the user's settings on first save and replaces them after that.
	 */
	public void save(final String municipalityId, final String adAccount, final UserSettings settings) {
		final var normalized = normalize(adAccount);
		final var entity = userSettingsRepository.findByMunicipalityIdAndAdAccount(municipalityId, normalized)
			.map(existing -> updateUserSettingsEntity(existing, settings))
			.orElseGet(() -> toUserSettingsEntity(settings, municipalityId, normalized));
		userSettingsRepository.save(entity);
	}

	/**
	 * Resets the user to the defaults. Idempotent: a user already on defaults is left as is.
	 */
	public void delete(final String municipalityId, final String adAccount) {
		userSettingsRepository.deleteByMunicipalityIdAndAdAccount(municipalityId, normalize(adAccount));
	}

	private static String normalize(final String adAccount) {
		return adAccount.toLowerCase(Locale.ROOT);
	}
}
