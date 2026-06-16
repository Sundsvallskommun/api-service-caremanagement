package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "A child pre-filled from Lifecare for a financial assistance renewal. Carries only what Lifecare provides — personnummer and name; the citizen completes residence, school etc. on the form.")
public class PrefilledChild {

	@Schema(description = "Party id (personId GUID) of the child", examples = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
	private String partyId;

	@Schema(description = "Name as registered in Lifecare", examples = "Kid Andersson")
	private String name;

	public static PrefilledChild create() {
		return new PrefilledChild();
	}

	public String getPartyId() {
		return partyId;
	}

	public void setPartyId(final String partyId) {
		this.partyId = partyId;
	}

	public PrefilledChild withPartyId(final String partyId) {
		this.partyId = partyId;
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
		return Objects.equals(partyId, that.partyId) && Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(partyId, name);
	}

	@Override
	public String toString() {
		return "PrefilledChild{partyId='" + partyId + "', name='" + name + "'}";
	}
}
