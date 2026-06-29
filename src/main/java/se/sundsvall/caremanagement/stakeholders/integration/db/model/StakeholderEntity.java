package se.sundsvall.caremanagement.stakeholders.integration.db.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;
import se.sundsvall.caremanagement.shared.Auditable;
import se.sundsvall.caremanagement.shared.AuditableListener;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * Stakeholder owned entirely by this module. Refers to the errand via a plain
 * {@code errand_id} column — no JPA back-reference to {@code ErrandEntity}.
 */
@Entity
@Table(name = "stakeholder",
	indexes = {
		@Index(name = "idx_stakeholder_external_id_role_errand_id", columnList = "external_id, `role`, errand_id"),
		@Index(name = "idx_stakeholder_errand_id", columnList = "errand_id")
	})
@EntityListeners(AuditableListener.class)
public class StakeholderEntity implements Auditable {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "errand_id", nullable = false, length = 255)
	private String errandId;

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

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	public static StakeholderEntity create() {
		return new StakeholderEntity();
	}

	public String getId() {
		return id;
	}

	public String getErrandId() {
		return errandId;
	}

	public String getExternalId() {
		return externalId;
	}

	public String getExternalIdType() {
		return externalIdType;
	}

	public String getRole() {
		return role;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getOrganizationName() {
		return organizationName;
	}

	public String getAddress() {
		return address;
	}

	public String getCareOf() {
		return careOf;
	}

	public String getZipCode() {
		return zipCode;
	}

	public String getCity() {
		return city;
	}

	public String getCountry() {
		return country;
	}

	public List<TagEmbeddable> getContactChannels() {
		return contactChannels;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public void setExternalId(final String externalId) {
		this.externalId = externalId;
	}

	public void setExternalIdType(final String externalIdType) {
		this.externalIdType = externalIdType;
	}

	public void setRole(final String role) {
		this.role = role;
	}

	public void setFirstName(final String firstName) {
		this.firstName = firstName;
	}

	public void setLastName(final String lastName) {
		this.lastName = lastName;
	}

	public void setOrganizationName(final String organizationName) {
		this.organizationName = organizationName;
	}

	public void setAddress(final String address) {
		this.address = address;
	}

	public void setCareOf(final String careOf) {
		this.careOf = careOf;
	}

	public void setZipCode(final String zipCode) {
		this.zipCode = zipCode;
	}

	public void setCity(final String city) {
		this.city = city;
	}

	public void setCountry(final String country) {
		this.country = country;
	}

	public void setContactChannels(final List<TagEmbeddable> contactChannels) {
		this.contactChannels = contactChannels;
	}

	@Override
	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	@Override
	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public StakeholderEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public StakeholderEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public StakeholderEntity withExternalId(final String externalId) {
		this.externalId = externalId;
		return this;
	}

	public StakeholderEntity withExternalIdType(final String externalIdType) {
		this.externalIdType = externalIdType;
		return this;
	}

	public StakeholderEntity withRole(final String role) {
		this.role = role;
		return this;
	}

	public StakeholderEntity withFirstName(final String firstName) {
		this.firstName = firstName;
		return this;
	}

	public StakeholderEntity withLastName(final String lastName) {
		this.lastName = lastName;
		return this;
	}

	public StakeholderEntity withOrganizationName(final String organizationName) {
		this.organizationName = organizationName;
		return this;
	}

	public StakeholderEntity withAddress(final String address) {
		this.address = address;
		return this;
	}

	public StakeholderEntity withCareOf(final String careOf) {
		this.careOf = careOf;
		return this;
	}

	public StakeholderEntity withZipCode(final String zipCode) {
		this.zipCode = zipCode;
		return this;
	}

	public StakeholderEntity withCity(final String city) {
		this.city = city;
		return this;
	}

	public StakeholderEntity withCountry(final String country) {
		this.country = country;
		return this;
	}

	public StakeholderEntity withContactChannels(final List<TagEmbeddable> contactChannels) {
		this.contactChannels = contactChannels;
		return this;
	}

	public StakeholderEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public StakeholderEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		final StakeholderEntity that = (StakeholderEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(errandId, that.errandId)
			&& Objects.equals(externalId, that.externalId) && Objects.equals(externalIdType, that.externalIdType)
			&& Objects.equals(role, that.role) && Objects.equals(firstName, that.firstName)
			&& Objects.equals(lastName, that.lastName) && Objects.equals(organizationName, that.organizationName)
			&& Objects.equals(address, that.address) && Objects.equals(careOf, that.careOf)
			&& Objects.equals(zipCode, that.zipCode) && Objects.equals(city, that.city)
			&& Objects.equals(country, that.country) && Objects.equals(contactChannels, that.contactChannels)
			&& Objects.equals(created, that.created) && Objects.equals(modified, that.modified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, externalId, externalIdType, role, firstName, lastName, organizationName,
			address, careOf, zipCode, city, country, contactChannels, created, modified);
	}

	@Override
	public String toString() {
		return "StakeholderEntity{id='" + id + "', errandId='" + errandId + "', role='" + role
			+ "', firstName='" + firstName + "', lastName='" + lastName + "', created=" + created + '}';
	}
}
