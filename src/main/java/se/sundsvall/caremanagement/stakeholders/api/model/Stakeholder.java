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

	@Schema(description = "Role of the stakeholder on the errand", examples = "FOSTER_PARENT")
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

	public void setId(final String id) {
		this.id = id;
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

	public void setContactChannels(final List<ContactChannel> contactChannels) {
		this.contactChannels = contactChannels;
	}

	public Stakeholder withId(final String id) {
		this.id = id;
		return this;
	}

	public Stakeholder withExternalId(final String externalId) {
		this.externalId = externalId;
		return this;
	}

	public Stakeholder withExternalIdType(final String externalIdType) {
		this.externalIdType = externalIdType;
		return this;
	}

	public Stakeholder withRole(final String role) {
		this.role = role;
		return this;
	}

	public Stakeholder withFirstName(final String firstName) {
		this.firstName = firstName;
		return this;
	}

	public Stakeholder withLastName(final String lastName) {
		this.lastName = lastName;
		return this;
	}

	public Stakeholder withOrganizationName(final String organizationName) {
		this.organizationName = organizationName;
		return this;
	}

	public Stakeholder withAddress(final String address) {
		this.address = address;
		return this;
	}

	public Stakeholder withCareOf(final String careOf) {
		this.careOf = careOf;
		return this;
	}

	public Stakeholder withZipCode(final String zipCode) {
		this.zipCode = zipCode;
		return this;
	}

	public Stakeholder withCity(final String city) {
		this.city = city;
		return this;
	}

	public Stakeholder withCountry(final String country) {
		this.country = country;
		return this;
	}

	public Stakeholder withContactChannels(final List<ContactChannel> contactChannels) {
		this.contactChannels = contactChannels;
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
