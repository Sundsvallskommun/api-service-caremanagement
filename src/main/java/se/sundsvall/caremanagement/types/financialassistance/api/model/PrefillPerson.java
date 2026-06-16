package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

@Schema(description = "A person pre-filled from Lifecare for a financial assistance renewal. Carries only what Lifecare provides — personnummer and name; the citizen completes the rest on the form.")
public class PrefillPerson {

	@Schema(description = "Role on the application; null for a household child", examples = "APPLICANT", allowableValues = {
		"APPLICANT", "CO_APPLICANT"
	})
	@OneOf(value = {
		"APPLICANT", "CO_APPLICANT"
	}, nullable = true)
	private String role;

	@Schema(description = "Personal number", examples = "198001012389")
	private String personalNumber;

	@Schema(description = "Name as registered in Lifecare", examples = "Anna Andersson")
	private String name;

	public static PrefillPerson create() {
		return new PrefillPerson();
	}

	public String getRole() {
		return role;
	}

	public void setRole(final String role) {
		this.role = role;
	}

	public PrefillPerson withRole(final String role) {
		this.role = role;
		return this;
	}

	public String getPersonalNumber() {
		return personalNumber;
	}

	public void setPersonalNumber(final String personalNumber) {
		this.personalNumber = personalNumber;
	}

	public PrefillPerson withPersonalNumber(final String personalNumber) {
		this.personalNumber = personalNumber;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public PrefillPerson withName(final String name) {
		this.name = name;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final PrefillPerson that = (PrefillPerson) o;
		return Objects.equals(role, that.role) && Objects.equals(personalNumber, that.personalNumber)
			&& Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(role, personalNumber, name);
	}

	@Override
	public String toString() {
		return "PrefillPerson{role='" + role + "', personalNumber='" + personalNumber + "', name='" + name + "'}";
	}
}
