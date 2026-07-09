package se.sundsvall.caremanagement.stakeholders.service.mapper;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.stakeholders.api.model.ContactChannel;
import se.sundsvall.caremanagement.stakeholders.api.model.Stakeholder;
import se.sundsvall.caremanagement.stakeholders.integration.db.model.StakeholderEntity;
import se.sundsvall.caremanagement.stakeholders.integration.db.model.TagEmbeddable;

import static org.assertj.core.api.Assertions.assertThat;

class StakeholderMapperTest {

	private static final String ID = "cb20c51f-fcf3-42c0-b613-de563634a8ec";
	private static final String ERRAND_ID = "e1d2c3b4-a5f6-7890-1234-567890abcdef";
	private static final String EXTERNAL_ID = "81471222-5798-11e9-ae24-57fa13b361e1";
	private static final String EXTERNAL_ID_TYPE = "PRIVATE";
	private static final String ROLE = "FOSTER_PARENT";
	private static final String FIRST_NAME = "Joe";
	private static final String LAST_NAME = "Doe";
	private static final String ORGANIZATION_NAME = "Sundsvalls kommun";
	private static final String ADDRESS = "Storgatan 1";
	private static final String CARE_OF = "c/o Doe";
	private static final String ZIP_CODE = "85248";
	private static final String CITY = "Sundsvall";
	private static final String COUNTRY = "Sweden";

	@Test
	void toStakeholderMapsEveryField() {
		final var entity = StakeholderEntity.create()
			.withId(ID)
			.withErrandId(ERRAND_ID)
			.withExternalId(EXTERNAL_ID)
			.withExternalIdType(EXTERNAL_ID_TYPE)
			.withRole(ROLE)
			.withFirstName(FIRST_NAME)
			.withLastName(LAST_NAME)
			.withOrganizationName(ORGANIZATION_NAME)
			.withAddress(ADDRESS)
			.withCareOf(CARE_OF)
			.withZipCode(ZIP_CODE)
			.withCity(CITY)
			.withCountry(COUNTRY)
			.withContactChannels(List.of(
				TagEmbeddable.create().withKey("EMAIL").withValue("joe.doe@example.com"),
				TagEmbeddable.create().withKey("PHONE").withValue("0701234567")));

		final var stakeholder = StakeholderMapper.toStakeholder(entity);

		assertThat(stakeholder).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(stakeholder.getId()).isEqualTo(ID);
		assertThat(stakeholder.getExternalId()).isEqualTo(EXTERNAL_ID);
		assertThat(stakeholder.getExternalIdType()).isEqualTo(EXTERNAL_ID_TYPE);
		assertThat(stakeholder.getRole()).isEqualTo(ROLE);
		assertThat(stakeholder.getFirstName()).isEqualTo(FIRST_NAME);
		assertThat(stakeholder.getLastName()).isEqualTo(LAST_NAME);
		assertThat(stakeholder.getOrganizationName()).isEqualTo(ORGANIZATION_NAME);
		assertThat(stakeholder.getAddress()).isEqualTo(ADDRESS);
		assertThat(stakeholder.getCareOf()).isEqualTo(CARE_OF);
		assertThat(stakeholder.getZipCode()).isEqualTo(ZIP_CODE);
		assertThat(stakeholder.getCity()).isEqualTo(CITY);
		assertThat(stakeholder.getCountry()).isEqualTo(COUNTRY);
		assertThat(stakeholder.getContactChannels()).hasSize(2);
		// TagEmbeddable -> ContactChannel: key/value round-trip
		assertThat(stakeholder.getContactChannels().get(0).getKey()).isEqualTo("EMAIL");
		assertThat(stakeholder.getContactChannels().get(0).getValue()).isEqualTo("joe.doe@example.com");
		assertThat(stakeholder.getContactChannels().get(1).getKey()).isEqualTo("PHONE");
		assertThat(stakeholder.getContactChannels().get(1).getValue()).isEqualTo("0701234567");
	}

	@Test
	void toStakeholderNullReturnsNull() {
		assertThat(StakeholderMapper.toStakeholder(null)).isNull();
	}

	@Test
	void toStakeholderEntityMapsEveryField() {
		final var stakeholder = Stakeholder.create()
			.withId(ID)
			.withExternalId(EXTERNAL_ID)
			.withExternalIdType(EXTERNAL_ID_TYPE)
			.withRole(ROLE)
			.withFirstName(FIRST_NAME)
			.withLastName(LAST_NAME)
			.withOrganizationName(ORGANIZATION_NAME)
			.withAddress(ADDRESS)
			.withCareOf(CARE_OF)
			.withZipCode(ZIP_CODE)
			.withCity(CITY)
			.withCountry(COUNTRY)
			.withContactChannels(List.of(
				ContactChannel.create().withKey("EMAIL").withValue("joe.doe@example.com"),
				ContactChannel.create().withKey("PHONE").withValue("0701234567")));

		final var entity = StakeholderMapper.toStakeholderEntity(stakeholder, ERRAND_ID);

		assertThat(entity).isNotNull().hasNoNullFieldsOrPropertiesExcept("id", "created", "modified");
		// errandId comes from the argument; id/created/modified are JPA-assigned and not carried over
		assertThat(entity.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(entity.getId()).isNull();
		assertThat(entity.getCreated()).isNull();
		assertThat(entity.getModified()).isNull();
		assertThat(entity.getExternalId()).isEqualTo(EXTERNAL_ID);
		assertThat(entity.getExternalIdType()).isEqualTo(EXTERNAL_ID_TYPE);
		assertThat(entity.getRole()).isEqualTo(ROLE);
		assertThat(entity.getFirstName()).isEqualTo(FIRST_NAME);
		assertThat(entity.getLastName()).isEqualTo(LAST_NAME);
		assertThat(entity.getOrganizationName()).isEqualTo(ORGANIZATION_NAME);
		assertThat(entity.getAddress()).isEqualTo(ADDRESS);
		assertThat(entity.getCareOf()).isEqualTo(CARE_OF);
		assertThat(entity.getZipCode()).isEqualTo(ZIP_CODE);
		assertThat(entity.getCity()).isEqualTo(CITY);
		assertThat(entity.getCountry()).isEqualTo(COUNTRY);
		assertThat(entity.getContactChannels()).hasSize(2);
		// ContactChannel -> TagEmbeddable: key/value round-trip
		assertThat(entity.getContactChannels().get(0).getKey()).isEqualTo("EMAIL");
		assertThat(entity.getContactChannels().get(0).getValue()).isEqualTo("joe.doe@example.com");
		assertThat(entity.getContactChannels().get(1).getKey()).isEqualTo("PHONE");
		assertThat(entity.getContactChannels().get(1).getValue()).isEqualTo("0701234567");
	}

	@Test
	void toStakeholderEntityNullContactChannelsMapsToEmptyList() {
		final var stakeholder = Stakeholder.create().withRole(ROLE).withContactChannels(null);

		final var entity = StakeholderMapper.toStakeholderEntity(stakeholder, ERRAND_ID);

		assertThat(entity).isNotNull();
		assertThat(entity.getContactChannels()).isEmpty();
	}

	@Test
	void toStakeholderEntityNullReturnsNull() {
		assertThat(StakeholderMapper.toStakeholderEntity(null, ERRAND_ID)).isNull();
	}

	@Test
	void updateStakeholderEntityAppliesAllTwelveFields() {
		final var entity = StakeholderEntity.create()
			.withId(ID)
			.withErrandId(ERRAND_ID)
			.withExternalId("old-external-id")
			.withExternalIdType("old-type")
			.withRole("OLD_ROLE")
			.withFirstName("OldFirst")
			.withLastName("OldLast")
			.withOrganizationName("Old org")
			.withAddress("Old address 9")
			.withCareOf("c/o Old")
			.withZipCode("00000")
			.withCity("Oldtown")
			.withCountry("Norway")
			.withContactChannels(List.of(TagEmbeddable.create().withKey("EMAIL").withValue("old@example.com")));

		final var source = Stakeholder.create()
			.withExternalId(EXTERNAL_ID)
			.withExternalIdType(EXTERNAL_ID_TYPE)
			.withRole(ROLE)
			.withFirstName(FIRST_NAME)
			.withLastName(LAST_NAME)
			.withOrganizationName(ORGANIZATION_NAME)
			.withAddress(ADDRESS)
			.withCareOf(CARE_OF)
			.withZipCode(ZIP_CODE)
			.withCity(CITY)
			.withCountry(COUNTRY)
			.withContactChannels(List.of(
				ContactChannel.create().withKey("PHONE").withValue("0709999999")));

		final var result = StakeholderMapper.updateStakeholderEntity(entity, source);

		// updates in place and returns the same instance
		assertThat(result).isSameAs(entity).hasNoNullFieldsOrPropertiesExcept("created", "modified");
		// untouched identity fields preserved
		assertThat(result.getId()).isEqualTo(ID);
		assertThat(result.getErrandId()).isEqualTo(ERRAND_ID);
		// all 12 updatable fields overwritten
		assertThat(result.getExternalId()).isEqualTo(EXTERNAL_ID);
		assertThat(result.getExternalIdType()).isEqualTo(EXTERNAL_ID_TYPE);
		assertThat(result.getRole()).isEqualTo(ROLE);
		assertThat(result.getFirstName()).isEqualTo(FIRST_NAME);
		assertThat(result.getLastName()).isEqualTo(LAST_NAME);
		assertThat(result.getOrganizationName()).isEqualTo(ORGANIZATION_NAME);
		assertThat(result.getAddress()).isEqualTo(ADDRESS);
		assertThat(result.getCareOf()).isEqualTo(CARE_OF);
		assertThat(result.getZipCode()).isEqualTo(ZIP_CODE);
		assertThat(result.getCity()).isEqualTo(CITY);
		assertThat(result.getCountry()).isEqualTo(COUNTRY);
		assertThat(result.getContactChannels()).hasSize(1);
		assertThat(result.getContactChannels().getFirst().getKey()).isEqualTo("PHONE");
		assertThat(result.getContactChannels().getFirst().getValue()).isEqualTo("0709999999");
	}

	@Test
	void updateStakeholderEntityLeavesNullSourceFieldsUntouched() {
		final var existingChannels = List.of(TagEmbeddable.create().withKey("EMAIL").withValue("keep@example.com"));
		final var entity = StakeholderEntity.create()
			.withId(ID)
			.withErrandId(ERRAND_ID)
			.withExternalId(EXTERNAL_ID)
			.withExternalIdType(EXTERNAL_ID_TYPE)
			.withRole(ROLE)
			.withFirstName(FIRST_NAME)
			.withLastName(LAST_NAME)
			.withOrganizationName(ORGANIZATION_NAME)
			.withAddress(ADDRESS)
			.withCareOf(CARE_OF)
			.withZipCode(ZIP_CODE)
			.withCity(CITY)
			.withCountry(COUNTRY)
			.withContactChannels(existingChannels);

		// PATCH: only firstName supplied — everything else null means "leave untouched"
		final var source = Stakeholder.create().withFirstName("Updated");

		final var result = StakeholderMapper.updateStakeholderEntity(entity, source);

		assertThat(result.getFirstName()).isEqualTo("Updated");
		assertThat(result.getExternalId()).isEqualTo(EXTERNAL_ID);
		assertThat(result.getExternalIdType()).isEqualTo(EXTERNAL_ID_TYPE);
		assertThat(result.getRole()).isEqualTo(ROLE);
		assertThat(result.getLastName()).isEqualTo(LAST_NAME);
		assertThat(result.getOrganizationName()).isEqualTo(ORGANIZATION_NAME);
		assertThat(result.getAddress()).isEqualTo(ADDRESS);
		assertThat(result.getCareOf()).isEqualTo(CARE_OF);
		assertThat(result.getZipCode()).isEqualTo(ZIP_CODE);
		assertThat(result.getCity()).isEqualTo(CITY);
		assertThat(result.getCountry()).isEqualTo(COUNTRY);
		// null contactChannels on source -> existing list untouched
		assertThat(result.getContactChannels()).isEqualTo(existingChannels);
	}

	@Test
	void updateStakeholderEntityNullEntityReturnsNull() {
		assertThat(StakeholderMapper.updateStakeholderEntity(null, Stakeholder.create())).isNull();
	}

	@Test
	void updateStakeholderEntityNullSourceReturnsEntityUnchanged() {
		final var entity = StakeholderEntity.create().withId(ID).withRole(ROLE);

		final var result = StakeholderMapper.updateStakeholderEntity(entity, null);

		assertThat(result).isSameAs(entity);
		assertThat(result.getRole()).isEqualTo(ROLE);
	}

	@Test
	void toStakeholderListMapsEveryItem() {
		final var first = StakeholderEntity.create().withId(ID).withRole(ROLE).withFirstName(FIRST_NAME);
		final var second = StakeholderEntity.create().withId("00000000-0000-0000-0000-000000000002").withRole("APPLICANT").withFirstName("Jane");

		final var result = StakeholderMapper.toStakeholderList(List.of(first, second));

		assertThat(result).hasSize(2);
		assertThat(result.get(0).getId()).isEqualTo(ID);
		assertThat(result.get(0).getRole()).isEqualTo(ROLE);
		assertThat(result.get(1).getId()).isEqualTo("00000000-0000-0000-0000-000000000002");
		assertThat(result.get(1).getFirstName()).isEqualTo("Jane");
	}

	@Test
	void toStakeholderListNullReturnsEmpty() {
		assertThat(StakeholderMapper.toStakeholderList(null)).isEmpty();
	}
}
