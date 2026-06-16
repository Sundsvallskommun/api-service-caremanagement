package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

@Schema(description = "A child included in the financial assistance application.")
public class Child {

	@Schema(description = "Party id (personId GUID) of the child", examples = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
	private String partyId;

	@Schema(description = "First name", examples = "Astrid")
	private String firstName;

	@Schema(description = "Last name", examples = "Lindgren")
	private String lastName;

	@Schema(description = "Name of the child's school", examples = "Bredsands skola")
	private String schoolName;

	@Schema(description = "Extent of residence in the home", examples = "FULL_TIME", allowableValues = {
		"FULL_TIME", "HALF_TIME", "OTHER"
	})
	@OneOf(value = {
		"FULL_TIME", "HALF_TIME", "OTHER"
	}, nullable = true)
	private String residenceExtent;

	@Schema(description = "Number of days per month the child lives in the home", examples = "15")
	private Integer daysInHome;

	public static Child create() {
		return new Child();
	}

	public String getPartyId() {
		return partyId;
	}

	public void setPartyId(final String partyId) {
		this.partyId = partyId;
	}

	public Child withPartyId(final String partyId) {
		this.partyId = partyId;
		return this;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(final String firstName) {
		this.firstName = firstName;
	}

	public Child withFirstName(final String firstName) {
		this.firstName = firstName;
		return this;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(final String lastName) {
		this.lastName = lastName;
	}

	public Child withLastName(final String lastName) {
		this.lastName = lastName;
		return this;
	}

	public String getSchoolName() {
		return schoolName;
	}

	public void setSchoolName(final String schoolName) {
		this.schoolName = schoolName;
	}

	public Child withSchoolName(final String schoolName) {
		this.schoolName = schoolName;
		return this;
	}

	public String getResidenceExtent() {
		return residenceExtent;
	}

	public void setResidenceExtent(final String residenceExtent) {
		this.residenceExtent = residenceExtent;
	}

	public Child withResidenceExtent(final String residenceExtent) {
		this.residenceExtent = residenceExtent;
		return this;
	}

	public Integer getDaysInHome() {
		return daysInHome;
	}

	public void setDaysInHome(final Integer daysInHome) {
		this.daysInHome = daysInHome;
	}

	public Child withDaysInHome(final Integer daysInHome) {
		this.daysInHome = daysInHome;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Child that = (Child) o;
		return Objects.equals(partyId, that.partyId) && Objects.equals(firstName, that.firstName)
			&& Objects.equals(lastName, that.lastName) && Objects.equals(schoolName, that.schoolName)
			&& Objects.equals(residenceExtent, that.residenceExtent) && Objects.equals(daysInHome, that.daysInHome);
	}

	@Override
	public int hashCode() {
		return Objects.hash(partyId, firstName, lastName, schoolName, residenceExtent, daysInHome);
	}

	@Override
	public String toString() {
		return "Child{partyId='" + partyId + "', firstName='" + firstName + "', lastName='" + lastName
			+ "', schoolName='" + schoolName + "', residenceExtent='" + residenceExtent + "', daysInHome=" + daysInHome + '}';
	}
}
