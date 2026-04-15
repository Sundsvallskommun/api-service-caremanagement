package se.sundsvall.caremanagement.integration.db.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.UuidGenerator;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "stakeholder",
	indexes = {
		@Index(name = "idx_stakeholder_external_id_role_errand_id", columnList = "external_id, `role`, errand_id")
	})
public class StakeholderEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "errand_id", nullable = false, foreignKey = @ForeignKey(name = "fk_stakeholder_errand_id"))
	private ErrandEntity errandEntity;

	@Column(name = "external_id")
	private String externalId;

	@Column(name = "external_id_type")
	private String externalIdType;

	@Column(name = "role")
	private String role;

	@Column(name = "first_name")
	private String firstName;

	@Column(name = "last_name")
	private String lastName;

	@Column(name = "organization_name")
	private String organizationName;

	@Column(name = "address")
	private String address;

	@Column(name = "care_of")
	private String careOf;

	@Column(name = "zip_code")
	private String zipCode;

	@Column(name = "city")
	private String city;

	@Column(name = "country")
	private String country;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
		name = "contact_channel",
		joinColumns = @JoinColumn(name = "stakeholder_id",
			referencedColumnName = "id",
			foreignKey = @ForeignKey(name = "fk_stakeholder_contact_channel_stakeholder_id")),
		indexes = {
			@Index(name = "idx_contact_channel_key_value", columnList = "\"key\", \"value\""),
			@Index(name = "idx_contact_channel_value", columnList = "\"value\"")
		})
	private List<TagEmbeddable> contactChannels;

	@OneToMany(mappedBy = "stakeholderEntity", cascade = ALL, orphanRemoval = true)
	private List<StakeholderParameterEntity> parameters;

	public static StakeholderEntity create() {
		return new StakeholderEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public StakeholderEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public ErrandEntity getErrandEntity() {
		return errandEntity;
	}

	public void setErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
	}

	public StakeholderEntity withErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
		return this;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(final String externalId) {
		this.externalId = externalId;
	}

	public StakeholderEntity withExternalId(final String externalId) {
		this.externalId = externalId;
		return this;
	}

	public String getExternalIdType() {
		return externalIdType;
	}

	public void setExternalIdType(final String externalIdType) {
		this.externalIdType = externalIdType;
	}

	public StakeholderEntity withExternalIdType(final String externalIdType) {
		this.externalIdType = externalIdType;
		return this;
	}

	public String getRole() {
		return role;
	}

	public void setRole(final String role) {
		this.role = role;
	}

	public StakeholderEntity withRole(final String role) {
		this.role = role;
		return this;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(final String firstName) {
		this.firstName = firstName;
	}

	public StakeholderEntity withFirstName(final String firstName) {
		this.firstName = firstName;
		return this;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(final String lastName) {
		this.lastName = lastName;
	}

	public StakeholderEntity withLastName(final String lastName) {
		this.lastName = lastName;
		return this;
	}

	public String getOrganizationName() {
		return organizationName;
	}

	public void setOrganizationName(final String organizationName) {
		this.organizationName = organizationName;
	}

	public StakeholderEntity withOrganizationName(final String organizationName) {
		this.organizationName = organizationName;
		return this;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(final String address) {
		this.address = address;
	}

	public StakeholderEntity withAddress(final String address) {
		this.address = address;
		return this;
	}

	public String getCareOf() {
		return careOf;
	}

	public void setCareOf(final String careOf) {
		this.careOf = careOf;
	}

	public StakeholderEntity withCareOf(final String careOf) {
		this.careOf = careOf;
		return this;
	}

	public String getZipCode() {
		return zipCode;
	}

	public void setZipCode(final String zipCode) {
		this.zipCode = zipCode;
	}

	public StakeholderEntity withZipCode(final String zipCode) {
		this.zipCode = zipCode;
		return this;
	}

	public String getCity() {
		return city;
	}

	public void setCity(final String city) {
		this.city = city;
	}

	public StakeholderEntity withCity(final String city) {
		this.city = city;
		return this;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(final String country) {
		this.country = country;
	}

	public StakeholderEntity withCountry(final String country) {
		this.country = country;
		return this;
	}

	public List<TagEmbeddable> getContactChannels() {
		return contactChannels;
	}

	public void setContactChannels(final List<TagEmbeddable> contactChannels) {
		this.contactChannels = contactChannels;
	}

	public StakeholderEntity withContactChannels(final List<TagEmbeddable> contactChannels) {
		this.contactChannels = contactChannels;
		return this;
	}

	public List<StakeholderParameterEntity> getParameters() {
		return parameters;
	}

	public void setParameters(final List<StakeholderParameterEntity> parameters) {
		this.parameters = parameters;
	}

	public StakeholderEntity withParameters(final List<StakeholderParameterEntity> parameters) {
		this.parameters = parameters;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final StakeholderEntity that = (StakeholderEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(externalId, that.externalId) && Objects.equals(externalIdType, that.externalIdType) && Objects.equals(role, that.role)
			&& Objects.equals(firstName, that.firstName) && Objects.equals(lastName, that.lastName) && Objects.equals(organizationName, that.organizationName) && Objects.equals(address, that.address) && Objects.equals(careOf, that.careOf)
			&& Objects.equals(zipCode, that.zipCode) && Objects.equals(city, that.city) && Objects.equals(country, that.country) && Objects.equals(contactChannels, that.contactChannels) && Objects.equals(parameters, that.parameters);
	}

	// errandEntity intentionally excluded to avoid infinite recursion (bidirectional relationship).
	@Override
	public int hashCode() {
		return Objects.hash(id, externalId, externalIdType, role, firstName, lastName, organizationName, address, careOf, zipCode, city, country, contactChannels, parameters);
	}

	// TODO(GDPR): encrypt externalId at-rest before exposing entity to production data.
	// TODO(GDPR): mask externalId/firstName/lastName at logging-layer (e.g. logback converter), NOT in toString
	// since BeanMatchers requires raw values for testing.
	@Override
	public String toString() {
		return "StakeholderEntity{" +
			"id='" + id + '\'' +
			", errandEntity=" + (errandEntity != null ? errandEntity.getId() : "null") +
			", externalId='" + externalId + '\'' +
			", externalIdType='" + externalIdType + '\'' +
			", role='" + role + '\'' +
			", firstName='" + firstName + '\'' +
			", lastName='" + lastName + '\'' +
			", organizationName='" + organizationName + '\'' +
			", address='" + address + '\'' +
			", careOf='" + careOf + '\'' +
			", zipCode='" + zipCode + '\'' +
			", city='" + city + '\'' +
			", country='" + country + '\'' +
			", contactChannels=" + contactChannels +
			", parameters=" + parameters +
			'}';
	}
}
