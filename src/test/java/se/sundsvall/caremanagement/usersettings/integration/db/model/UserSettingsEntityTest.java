package se.sundsvall.caremanagement.usersettings.integration.db.model;

import java.time.OffsetDateTime;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class UserSettingsEntityTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(UserSettingsEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var adAccount = "joe01doe";
		final var created = FIXED_TIMESTAMP.minusDays(1);
		final var id = 1L;
		final var modified = FIXED_TIMESTAMP;
		final var municipalityId = "2281";
		final var ssbtekOpenInNewWindow = false;

		final var entity = UserSettingsEntity.create()
			.withAdAccount(adAccount)
			.withCreated(created)
			.withId(id)
			.withModified(modified)
			.withMunicipalityId(municipalityId)
			.withSsbtekOpenInNewWindow(ssbtekOpenInNewWindow);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getAdAccount()).isEqualTo(adAccount);
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getModified()).isEqualTo(modified);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getSsbtekOpenInNewWindow()).isEqualTo(ssbtekOpenInNewWindow);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(UserSettingsEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new UserSettingsEntity()).hasAllNullFieldsOrProperties();
	}
}
