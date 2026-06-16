package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class FaChild {

	@Column(name = "party_id")
	private String partyId;

	@Column(name = "first_name")
	private String firstName;

	@Column(name = "last_name")
	private String lastName;

	@Column(name = "school_name")
	private String schoolName;

	@Column(name = "residence_extent")
	private String residenceExtent;

	@Column(name = "days_in_home")
	private Integer daysInHome;

	public static FaChild create() {
		return new FaChild();
	}

	public String getPartyId() {
		return partyId;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getSchoolName() {
		return schoolName;
	}

	public String getResidenceExtent() {
		return residenceExtent;
	}

	public Integer getDaysInHome() {
		return daysInHome;
	}

	public void setPartyId(final String partyId) {
		this.partyId = partyId;
	}

	public void setFirstName(final String firstName) {
		this.firstName = firstName;
	}

	public void setLastName(final String lastName) {
		this.lastName = lastName;
	}

	public void setSchoolName(final String schoolName) {
		this.schoolName = schoolName;
	}

	public void setResidenceExtent(final String residenceExtent) {
		this.residenceExtent = residenceExtent;
	}

	public void setDaysInHome(final Integer daysInHome) {
		this.daysInHome = daysInHome;
	}

	public FaChild withPartyId(final String partyId) {
		this.partyId = partyId;
		return this;
	}

	public FaChild withFirstName(final String firstName) {
		this.firstName = firstName;
		return this;
	}

	public FaChild withLastName(final String lastName) {
		this.lastName = lastName;
		return this;
	}

	public FaChild withSchoolName(final String schoolName) {
		this.schoolName = schoolName;
		return this;
	}

	public FaChild withResidenceExtent(final String residenceExtent) {
		this.residenceExtent = residenceExtent;
		return this;
	}

	public FaChild withDaysInHome(final Integer daysInHome) {
		this.daysInHome = daysInHome;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FaChild that = (FaChild) o;
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
		return "FaChild{partyId='" + partyId + "', firstName='" + firstName + "', lastName='" + lastName
			+ "', schoolName='" + schoolName + "', residenceExtent='" + residenceExtent + "', daysInHome=" + daysInHome + '}';
	}
}
