package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/**
 * Pre-fill for a financial assistance renewal (återansökan), distilled from the applicant's most recent Lifecare
 * normberäkning and beslut. The {@code persons} are the sökande and — when applying together — the medsökande; the
 * {@code children} are the remaining household members. Lifecare only supplies personnummer + name, so the rest of each
 * row (boendeomfattning, skola, utbetalning …) is left for the citizen. Best-effort: {@code lifecareChecked} is false
 * when Lifecare could not be reached, in which case the lists are empty.
 */
@Schema(description = "Pre-fill data for a financial assistance renewal (återansökan), read from Lifecare.")
public class RenewalPrefill {

	@Schema(description = "The applicant and, when applying together, the co-applicant (medsökande)")
	private List<PrefillPerson> persons;

	@Schema(description = "Children in the household from the most recent normberäkning")
	private List<PrefillPerson> children;

	@Schema(description = "True when the Lifecare lookup succeeded. False means the answer is degraded (empty lists).", examples = "true")
	private boolean lifecareChecked;

	public static RenewalPrefill create() {
		return new RenewalPrefill();
	}

	public List<PrefillPerson> getPersons() {
		return persons;
	}

	public void setPersons(final List<PrefillPerson> persons) {
		this.persons = persons;
	}

	public RenewalPrefill withPersons(final List<PrefillPerson> persons) {
		this.persons = persons;
		return this;
	}

	public List<PrefillPerson> getChildren() {
		return children;
	}

	public void setChildren(final List<PrefillPerson> children) {
		this.children = children;
	}

	public RenewalPrefill withChildren(final List<PrefillPerson> children) {
		this.children = children;
		return this;
	}

	public boolean isLifecareChecked() {
		return lifecareChecked;
	}

	public void setLifecareChecked(final boolean lifecareChecked) {
		this.lifecareChecked = lifecareChecked;
	}

	public RenewalPrefill withLifecareChecked(final boolean lifecareChecked) {
		this.lifecareChecked = lifecareChecked;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final RenewalPrefill that = (RenewalPrefill) o;
		return lifecareChecked == that.lifecareChecked && Objects.equals(persons, that.persons)
			&& Objects.equals(children, that.children);
	}

	@Override
	public int hashCode() {
		return Objects.hash(persons, children, lifecareChecked);
	}

	@Override
	public String toString() {
		return "RenewalPrefill{persons=" + persons + ", children=" + children + ", lifecareChecked=" + lifecareChecked + '}';
	}
}
