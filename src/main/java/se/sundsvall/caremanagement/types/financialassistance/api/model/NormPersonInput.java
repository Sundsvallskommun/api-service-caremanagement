package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * What a handläggare sends to add a new person row (origin HANDLAGGARE) or patch an existing one — only the handläggare
 * days + note are honoured on a patch.
 */
@Schema(description = "What a handläggare sends to add or patch a person row (identity + handläggare-writable fields only).")
public class NormPersonInput {

	@Schema(description = "The party id of the household member")
	private String partyId;

	@Schema(description = "The role of the household member", allowableValues = {
		"APPLICANT", "CO_APPLICANT", "CHILD"
	})
	private String role;

	@Schema(description = "The name of the household member")
	private String name;

	@Schema(description = "The number of days the handläggare decided", examples = "15")
	private Integer handlaggareDays;

	@Schema(description = "Free-text note")
	private String note;

	public static NormPersonInput create() {
		return new NormPersonInput();
	}

	public String getPartyId() {
		return partyId;
	}

	public void setPartyId(final String partyId) {
		this.partyId = partyId;
	}

	public NormPersonInput withPartyId(final String partyId) {
		this.partyId = partyId;
		return this;
	}

	public String getRole() {
		return role;
	}

	public void setRole(final String role) {
		this.role = role;
	}

	public NormPersonInput withRole(final String role) {
		this.role = role;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public NormPersonInput withName(final String name) {
		this.name = name;
		return this;
	}

	public Integer getHandlaggareDays() {
		return handlaggareDays;
	}

	public void setHandlaggareDays(final Integer handlaggareDays) {
		this.handlaggareDays = handlaggareDays;
	}

	public NormPersonInput withHandlaggareDays(final Integer handlaggareDays) {
		this.handlaggareDays = handlaggareDays;
		return this;
	}

	public String getNote() {
		return note;
	}

	public void setNote(final String note) {
		this.note = note;
	}

	public NormPersonInput withNote(final String note) {
		this.note = note;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final NormPersonInput that = (NormPersonInput) o;
		return Objects.equals(partyId, that.partyId) && Objects.equals(role, that.role) && Objects.equals(name, that.name)
			&& Objects.equals(handlaggareDays, that.handlaggareDays) && Objects.equals(note, that.note);
	}

	@Override
	public int hashCode() {
		return Objects.hash(partyId, role, name, handlaggareDays, note);
	}

	@Override
	public String toString() {
		return "NormPersonInput{" +
			"partyId='" + partyId + '\'' +
			", role='" + role + '\'' +
			", name='" + name + '\'' +
			", handlaggareDays=" + handlaggareDays +
			", note='" + note + '\'' +
			'}';
	}
}
