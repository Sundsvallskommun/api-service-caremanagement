package se.sundsvall.caremanagement.stakeholders.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import java.util.List;
import java.util.Objects;
import se.sundsvall.caremanagement.core.api.validation.groups.OnCreate;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * Slim stakeholder DTO. The parameter swamp has been deleted — if a type-specific need
 * surfaces, that data goes on the type module's own table, not here.
 */
@Schema(description = "Stakeholder")
public class Stakeholder {

	@Schema(description = "Unique identifier", examples = "cb20c51f-fcf3-42c0-b613-de563634a8ec", accessMode = READ_ONLY)
	@Null(groups = OnCreate.class)
	private String id;

	@Schema(description = "External id for the stakeholder", examples = "81471222-5798-11e9-ae24-57fa13b361e1")
	private String externalId;

	@Schema(description = "Type of external id", examples = "PRIVATE")
	private String externalIdType;

	@Schema(description = "Role of the stakeholder — validated against StakeholderRoleRegistry for the errand's typeSlug",
		examples = "FOSTER_PARENT")
	@NotBlank(groups = OnCreate.class)
	private String role;

	@Schema(description = "First name", examples = "Joe")
	private String firstName;

	@Schema(description = "Last name", examples = "Doe")
	private String lastName;

	@Schema(description = "Organization name", examples = "Sundsvalls kommun")
	private String organizationName;

	@Schema(description = "Address", examples = "Storgatan 1")
	private String address;

	@Schema(description = "Care of", examples = "c/o Doe")
	private String careOf;

	@Schema(description = "Zip code", examples = "85248")
	private String zipCode;

	@Schema(description = "City", examples = "Sundsvall")
	private String city;

	@Schema(description = "Country", examples = "Sweden")
	private String country;

	@Schema(description = "Contact channels for the stakeholder")
	@Valid
	private List<ContactChannel> contactChannels;

	public static Stakeholder create() {
		return new Stakeholder();
	}

	public String getId() {
		return id;
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

	public List<ContactChannel> getContactChannels() {
		return contactChannels;
	}

	public void setId(final String v) {
		this.id = v;
	}

	public void setExternalId(final String v) {
		this.externalId = v;
	}

	public void setExternalIdType(final String v) {
		this.externalIdType = v;
	}

	public void setRole(final String v) {
		this.role = v;
	}

	public void setFirstName(final String v) {
		this.firstName = v;
	}

	public void setLastName(final String v) {
		this.lastName = v;
	}

	public void setOrganizationName(final String v) {
		this.organizationName = v;
	}

	public void setAddress(final String v) {
		this.address = v;
	}

	public void setCareOf(final String v) {
		this.careOf = v;
	}

	public void setZipCode(final String v) {
		this.zipCode = v;
	}

	public void setCity(final String v) {
		this.city = v;
	}

	public void setCountry(final String v) {
		this.country = v;
	}

	public void setContactChannels(final List<ContactChannel> v) {
		this.contactChannels = v;
	}

	public Stakeholder withId(final String v) {
		this.id = v;
		return this;
	}

	public Stakeholder withExternalId(final String v) {
		this.externalId = v;
		return this;
	}

	public Stakeholder withExternalIdType(final String v) {
		this.externalIdType = v;
		return this;
	}

	public Stakeholder withRole(final String v) {
		this.role = v;
		return this;
	}

	public Stakeholder withFirstName(final String v) {
		this.firstName = v;
		return this;
	}

	public Stakeholder withLastName(final String v) {
		this.lastName = v;
		return this;
	}

	public Stakeholder withOrganizationName(final String v) {
		this.organizationName = v;
		return this;
	}

	public Stakeholder withAddress(final String v) {
		this.address = v;
		return this;
	}

	public Stakeholder withCareOf(final String v) {
		this.careOf = v;
		return this;
	}

	public Stakeholder withZipCode(final String v) {
		this.zipCode = v;
		return this;
	}

	public Stakeholder withCity(final String v) {
		this.city = v;
		return this;
	}

	public Stakeholder withCountry(final String v) {
		this.country = v;
		return this;
	}

	public Stakeholder withContactChannels(final List<ContactChannel> v) {
		this.contactChannels = v;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final Stakeholder that = (Stakeholder) o;
		return Objects.equals(id, that.id) && Objects.equals(externalId, that.externalId) && Objects.equals(externalIdType, that.externalIdType)
			&& Objects.equals(role, that.role) && Objects.equals(firstName, that.firstName) && Objects.equals(lastName, that.lastName)
			&& Objects.equals(organizationName, that.organizationName) && Objects.equals(address, that.address) && Objects.equals(careOf, that.careOf)
			&& Objects.equals(zipCode, that.zipCode) && Objects.equals(city, that.city) && Objects.equals(country, that.country)
			&& Objects.equals(contactChannels, that.contactChannels);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, externalId, externalIdType, role, firstName, lastName, organizationName, address, careOf, zipCode, city, country, contactChannels);
	}

	@Override
	public String toString() {
		return "Stakeholder{" +
			"id='" + id + '\'' +
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
			'}';
	}
}
