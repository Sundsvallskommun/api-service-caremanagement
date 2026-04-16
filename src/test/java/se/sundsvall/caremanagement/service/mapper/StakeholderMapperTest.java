package se.sundsvall.caremanagement.service.mapper;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.api.model.ContactChannel;
import se.sundsvall.caremanagement.api.model.Stakeholder;
import se.sundsvall.caremanagement.api.model.StakeholderParameter;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.caremanagement.integration.db.model.StakeholderParameterEntity;
import se.sundsvall.caremanagement.integration.db.model.TagEmbeddable;

import static org.assertj.core.api.Assertions.assertThat;

class StakeholderMapperTest {

	@Test
	void toStakeholder_maps() {
		final var entity = StakeholderEntity.create()
			.withId("sid")
			.withExternalId("xid")
			.withExternalIdType("PRIVATE")
			.withRole("APPLICANT")
			.withFirstName("Joe")
			.withLastName("Doe")
			.withOrganizationName("ACME")
			.withAddress("Street 1")
			.withCareOf("c/o Doe")
			.withZipCode("85248")
			.withCity("Sundsvall")
			.withCountry("Sweden")
			.withContactChannels(new ArrayList<>(List.of(TagEmbeddable.create().withKey("PHONE").withValue("123"))))
			.withParameters(new ArrayList<>(List.of(
				StakeholderParameterEntity.create().withId(1L).withKey("k"))));

		final var result = StakeholderMapper.toStakeholder(entity);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo("sid");
		assertThat(result.getExternalId()).isEqualTo("xid");
		assertThat(result.getExternalIdType()).isEqualTo("PRIVATE");
		assertThat(result.getRole()).isEqualTo("APPLICANT");
		assertThat(result.getFirstName()).isEqualTo("Joe");
		assertThat(result.getLastName()).isEqualTo("Doe");
		assertThat(result.getOrganizationName()).isEqualTo("ACME");
		assertThat(result.getAddress()).isEqualTo("Street 1");
		assertThat(result.getCareOf()).isEqualTo("c/o Doe");
		assertThat(result.getZipCode()).isEqualTo("85248");
		assertThat(result.getCity()).isEqualTo("Sundsvall");
		assertThat(result.getCountry()).isEqualTo("Sweden");
		assertThat(result.getContactChannels()).hasSize(1);
		assertThat(result.getParameters()).hasSize(1);
	}

	@Test
	void toStakeholder_nullReturnsNull() {
		assertThat(StakeholderMapper.toStakeholder(null)).isNull();
	}

	@Test
	void toStakeholderEntity_maps() {
		final var errand = ErrandEntity.create().withId("eid");
		final var stakeholder = Stakeholder.create()
			.withExternalId("xid")
			.withExternalIdType("PRIVATE")
			.withRole("APPLICANT")
			.withFirstName("Joe")
			.withLastName("Doe")
			.withOrganizationName("ACME")
			.withAddress("Street 1")
			.withCareOf("c/o Doe")
			.withZipCode("85248")
			.withCity("Sundsvall")
			.withCountry("Sweden")
			.withContactChannels(List.of(ContactChannel.create().withKey("PHONE").withValue("123")))
			.withParameters(List.of(StakeholderParameter.create().withKey("k").withValues(List.of("v"))));

		final var result = StakeholderMapper.toStakeholderEntity(stakeholder, errand);

		assertThat(result).isNotNull();
		assertThat(result.getErrandEntity()).isSameAs(errand);
		assertThat(result.getExternalId()).isEqualTo("xid");
		assertThat(result.getRole()).isEqualTo("APPLICANT");
		assertThat(result.getContactChannels()).hasSize(1);
		assertThat(result.getParameters()).hasSize(1);
		assertThat(result.getParameters().getFirst().getStakeholderEntity()).isSameAs(result);
	}

	@Test
	void toStakeholderEntity_nullReturnsNull() {
		assertThat(StakeholderMapper.toStakeholderEntity(null, ErrandEntity.create())).isNull();
	}

	@Test
	void updateStakeholderEntity_updates() {
		final var entity = StakeholderEntity.create()
			.withExternalId("old")
			.withContactChannels(new ArrayList<>(List.of(TagEmbeddable.create().withKey("OLD").withValue("x"))));
		final var source = Stakeholder.create()
			.withExternalId("new")
			.withExternalIdType("PRIVATE")
			.withRole("APPLICANT")
			.withFirstName("Joe")
			.withLastName("Doe")
			.withOrganizationName("ACME")
			.withAddress("Street 1")
			.withCareOf("c/o")
			.withZipCode("85248")
			.withCity("Sundsvall")
			.withCountry("Sweden")
			.withContactChannels(List.of(ContactChannel.create().withKey("PHONE").withValue("123")));

		final var result = StakeholderMapper.updateStakeholderEntity(entity, source);

		assertThat(result).isSameAs(entity);
		assertThat(result.getExternalId()).isEqualTo("new");
		assertThat(result.getContactChannels()).hasSize(1);
		assertThat(result.getContactChannels().getFirst().getKey()).isEqualTo("PHONE");
	}

	@Test
	void updateStakeholderEntity_nullEntity() {
		assertThat(StakeholderMapper.updateStakeholderEntity(null, Stakeholder.create())).isNull();
	}

	@Test
	void updateStakeholderEntity_nullSource() {
		final var entity = StakeholderEntity.create().withExternalId("kept");
		final var result = StakeholderMapper.updateStakeholderEntity(entity, null);
		assertThat(result).isSameAs(entity);
		assertThat(result.getExternalId()).isEqualTo("kept");
	}

	@Test
	void toStakeholderList_maps() {
		final var result = StakeholderMapper.toStakeholderList(List.of(
			StakeholderEntity.create().withId("a"),
			StakeholderEntity.create().withId("b")));

		assertThat(result).hasSize(2);
	}

	@Test
	void toStakeholderList_nullReturnsEmpty() {
		assertThat(StakeholderMapper.toStakeholderList(null)).isEmpty();
	}

	@Test
	void toStakeholderEntityList_maps() {
		final var errand = ErrandEntity.create().withId("eid");
		final var result = StakeholderMapper.toStakeholderEntityList(List.of(
			Stakeholder.create().withRole("APPLICANT"),
			Stakeholder.create().withRole("CONTACT")), errand);

		assertThat(result).hasSize(2);
		assertThat(result.getFirst().getErrandEntity()).isSameAs(errand);
	}

	@Test
	void toStakeholderEntityList_nullReturnsEmpty() {
		assertThat(StakeholderMapper.toStakeholderEntityList(null, ErrandEntity.create())).isEmpty();
	}
}
