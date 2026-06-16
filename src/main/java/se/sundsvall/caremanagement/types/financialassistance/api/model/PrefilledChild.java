package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "A child pre-filled from Lifecare for a financial assistance renewal. Carries only what Lifecare provides — personnummer and name; the citizen completes residence, school etc. on the form.")
public class PrefilledChild {

	@Schema(description = "Personal number", examples = "201801012380")
	private String personalNumber;

	@Schema(description = "Name as registered in Lifecare", examples = "Kid Andersson")
	private String name;

	public static PrefilledChild create() {
		return new PrefilledChild();
	}

	public String getPersonalNumber() {
		return personalNumber;
	}

	public void setPersonalNumber(final String personalNumber) {
		this.personalNumber = personalNumber;
	}

	public PrefilledChild withPersonalNumber(final String personalNumber) {
		this.personalNumber = personalNumber;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public PrefilledChild withName(final String name) {
		this.name = name;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final PrefilledChild that = (PrefilledChild) o;
		return Objects.equals(personalNumber, that.personalNumber) && Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(personalNumber, name);
	}

	@Override
	public String toString() {
		return "PrefilledChild{personalNumber='" + personalNumber + "', name='" + name + "'}";
	}
}
