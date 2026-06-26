package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * A single person on a Lifecare beslut — identifies the person and whether they are the co-applicant.
 */
@Schema(description = "A person on a Lifecare decision.")
public class LifecareDecisionPerson {

	@Schema(description = "The Lifecare person id", examples = "200001011234")
	private String personId;

	@Schema(description = "The person name", examples = "Anna Andersson")
	private String name;

	@Schema(description = "Whether the person is the co-applicant", examples = "false")
	private Boolean coApplicant;

	public static LifecareDecisionPerson create() {
		return new LifecareDecisionPerson();
	}

	public String getPersonId() {
		return personId;
	}

	public void setPersonId(final String personId) {
		this.personId = personId;
	}

	public LifecareDecisionPerson withPersonId(final String personId) {
		this.personId = personId;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public LifecareDecisionPerson withName(final String name) {
		this.name = name;
		return this;
	}

	public Boolean getCoApplicant() {
		return coApplicant;
	}

	public void setCoApplicant(final Boolean coApplicant) {
		this.coApplicant = coApplicant;
	}

	public LifecareDecisionPerson withCoApplicant(final Boolean coApplicant) {
		this.coApplicant = coApplicant;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final LifecareDecisionPerson that = (LifecareDecisionPerson) o;
		return Objects.equals(personId, that.personId) && Objects.equals(name, that.name) && Objects.equals(coApplicant, that.coApplicant);
	}

	@Override
	public int hashCode() {
		return Objects.hash(personId, name, coApplicant);
	}

	@Override
	public String toString() {
		return "LifecareDecisionPerson{personId='" + personId + "', name='" + name + "', coApplicant=" + coApplicant + "}";
	}
}
