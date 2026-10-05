package se.sundsvall.caremanagement.usersettings.service;

import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.usersettings.api.model.UserSettings;
import se.sundsvall.caremanagement.usersettings.integration.db.UserSettingsRepository;
import se.sundsvall.caremanagement.usersettings.integration.db.model.UserSettingsEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSettingsServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String AD_ACCOUNT = "JOE01DOE";
	private static final String NORMALIZED_AD_ACCOUNT = "joe01doe";

	@Mock
	private UserSettingsRepository repositoryMock;

	@Captor
	private ArgumentCaptor<UserSettingsEntity> entityCaptor;

	@InjectMocks
	private UserSettingsService service;

	@AfterEach
	void verifyNoMoreMockInteractions() {
		verifyNoMoreInteractions(repositoryMock);
	}

	@Test
	void readStored() {
		final var entity = UserSettingsEntity.create().withAdAccount(NORMALIZED_AD_ACCOUNT).withSsbtekOpenInNewWindow(false);
		when(repositoryMock.findByMunicipalityIdAndAdAccount(MUNICIPALITY_ID, NORMALIZED_AD_ACCOUNT)).thenReturn(Optional.of(entity));

		final var result = service.read(MUNICIPALITY_ID, AD_ACCOUNT);

		assertThat(result.getAdAccount()).isEqualTo(NORMALIZED_AD_ACCOUNT);
		assertThat(result.getSsbtekOpenInNewWindow()).isFalse();
		verify(repositoryMock).findByMunicipalityIdAndAdAccount(MUNICIPALITY_ID, NORMALIZED_AD_ACCOUNT);
	}

	@Test
	void readDefaultsWhenNothingStored() {
		when(repositoryMock.findByMunicipalityIdAndAdAccount(MUNICIPALITY_ID, NORMALIZED_AD_ACCOUNT)).thenReturn(Optional.empty());

		final var result = service.read(MUNICIPALITY_ID, AD_ACCOUNT);

		assertThat(result).isEqualTo(UserSettings.create().withAdAccount(NORMALIZED_AD_ACCOUNT).withSsbtekOpenInNewWindow(true));
		verify(repositoryMock).findByMunicipalityIdAndAdAccount(MUNICIPALITY_ID, NORMALIZED_AD_ACCOUNT);
	}

	@Test
	void saveCreatesWhenNothingStored() {
		when(repositoryMock.findByMunicipalityIdAndAdAccount(MUNICIPALITY_ID, NORMALIZED_AD_ACCOUNT)).thenReturn(Optional.empty());

		service.save(MUNICIPALITY_ID, AD_ACCOUNT, UserSettings.create().withSsbtekOpenInNewWindow(false));

		verify(repositoryMock).findByMunicipalityIdAndAdAccount(MUNICIPALITY_ID, NORMALIZED_AD_ACCOUNT);
		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(entityCaptor.getValue().getAdAccount()).isEqualTo(NORMALIZED_AD_ACCOUNT);
		assertThat(entityCaptor.getValue().getSsbtekOpenInNewWindow()).isFalse();
	}

	@Test
	void saveReplacesStored() {
		final var entity = UserSettingsEntity.create().withId(1L).withMunicipalityId(MUNICIPALITY_ID).withAdAccount(NORMALIZED_AD_ACCOUNT).withSsbtekOpenInNewWindow(true);
		when(repositoryMock.findByMunicipalityIdAndAdAccount(MUNICIPALITY_ID, NORMALIZED_AD_ACCOUNT)).thenReturn(Optional.of(entity));

		service.save(MUNICIPALITY_ID, AD_ACCOUNT, UserSettings.create().withSsbtekOpenInNewWindow(false));

		verify(repositoryMock).findByMunicipalityIdAndAdAccount(MUNICIPALITY_ID, NORMALIZED_AD_ACCOUNT);
		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue()).isSameAs(entity);
		assertThat(entity.getId()).isEqualTo(1L);
		assertThat(entity.getSsbtekOpenInNewWindow()).isFalse();
	}

	@Test
	void delete() {
		service.delete(MUNICIPALITY_ID, AD_ACCOUNT);

		verify(repositoryMock).deleteByMunicipalityIdAndAdAccount(MUNICIPALITY_ID, NORMALIZED_AD_ACCOUNT);
	}
}
