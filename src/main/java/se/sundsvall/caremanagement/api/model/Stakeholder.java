package se.sundsvall.caremanagement.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import java.util.List;
import java.util.Objects;
import se.sundsvall.caremanagement.api.validation.groups.OnCreate;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Stakeholder model")
public class Stakeholder {

	@Schema(description = "Unique identifier", examples = "cb20c51f-fcf3-42c0-b613-de563634a8ec", accessMode = READ_ONLY)
	@Null(groups = OnCreate.class)
	private String id;

	@Schema(description = "External id for the stakeholder", examples = "81471222-5798-11e9-ae24-57fa13b361e1")
	private String externalId;

	@Schema(description = "Type of external id", examples = "PRIVATE")
	private String externalIdType;

	@Schema(description = "Role of the stakeholder", examples = "APPLICANT")
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

	@Schema(description = "Parameters for the stakeholder")
	@Valid
	private List<StakeholderParameter> parameters;

	public static Stakeholder create() {
		return new Stakeholder();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Stakeholder withId(final String id) {
		this.id = id;
		return this;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(final String externalId) {
		this.externalId = externalId;
	}

	public Stakeholder withExternalId(final String externalId) {
		this.externalId = externalId;
		return this;
	}

	public String getExternalIdType() {
		return externalIdType;
	}

	public void setExternalIdType(final String externalIdType) {
		this.externalIdType = externalIdType;
	}

	public Stakeholder withExternalIdType(final String externalIdType) {
		this.externalIdType = externalIdType;
		return this;
	}

	public String getRole() {
		return role;
	}

	public void setRole(final String role) {
		this.role = role;
	}

	public Stakeholder withRole(final String role) {
		this.role = role;
		return this;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(final String firstName) {
		this.firstName = firstName;
	}

	public Stakeholder withFirstName(final String firstName) {
		this.firstName = firstName;
		return this;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(final String lastName) {
		this.lastName = lastName;
	}

	public Stakeholder withLastName(final String lastName) {
		this.lastName = lastName;
		return this;
	}

	public String getOrganizationName() {
		return organizationName;
	}

	public void setOrganizationName(final String organizationName) {
		this.organizationName = organizationName;
	}

	public Stakeholder withOrganizationName(final String organizationName) {
		this.organizationName = organizationName;
		return this;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(final String address) {
		this.address = address;
	}

	public Stakeholder withAddress(final String address) {
		this.address = address;
		return this;
	}

	public String getCareOf() {
		return careOf;
	}

	public void setCareOf(final String careOf) {
		this.careOf = careOf;
	}

	public Stakeholder withCareOf(final String careOf) {
		this.careOf = careOf;
		return this;
	}

	public String getZipCode() {
		return zipCode;
	}

	public void setZipCode(final String zipCode) {
		this.zipCode = zipCode;
	}

	public Stakeholder withZipCode(final String zipCode) {
		this.zipCode = zipCode;
		return this;
	}

	public String getCity() {
		return city;
	}

	public void setCity(final String city) {
		this.city = city;
	}

	public Stakeholder withCity(final String city) {
		this.city = city;
		return this;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(final String country) {
		this.country = country;
	}

	public Stakeholder withCountry(final String country) {
		this.country = country;
		return this;
	}

	public List<ContactChannel> getContactChannels() {
		return contactChannels;
	}

	public void setContactChannels(final List<ContactChannel> contactChannels) {
		this.contactChannels = contactChannels;
	}

	public Stakeholder withContactChannels(final List<ContactChannel> contactChannels) {
		this.contactChannels = contactChannels;
		return this;
	}

	public List<StakeholderParameter> getParameters() {
		return parameters;
	}

	public void setParameters(final List<StakeholderParameter> parameters) {
		this.parameters = parameters;
	}

	public Stakeholder withParameters(final List<StakeholderParameter> parameters) {
		this.parameters = parameters;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Stakeholder that = (Stakeholder) o;
		return Objects.equals(id, that.id) && Objects.equals(externalId, that.externalId) && Objects.equals(externalIdType, that.externalIdType) && Objects.equals(role, that.role)
			&& Objects.equals(firstName, that.firstName) && Objects.equals(lastName, that.lastName) && Objects.equals(organizationName, that.organizationName) && Objects.equals(address, that.address)
			&& Objects.equals(careOf, that.careOf) && Objects.equals(zipCode, that.zipCode) && Objects.equals(city, that.city) && Objects.equals(country, that.country)
			&& Objects.equals(contactChannels, that.contactChannels) && Objects.equals(parameters, that.parameters);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, externalId, externalIdType, role, firstName, lastName, organizationName, address, careOf, zipCode, city, country, contactChannels, parameters);
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
			", parameters=" + parameters +
			'}';
	}
}
