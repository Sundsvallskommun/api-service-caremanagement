package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/**
 * Pre-fill for a financial assistance renewal (renewal): the children in the applicant's household, read from the
 * most recent Lifecare calculation. Only children are pre-filled — the applicant is the logged-in citizen and the
 * co-applicant comes from the portal, so neither is returned here. Lifecare only supplies personnummer + name, so the
 * rest of each child (housingomfattning, skola …) is left for the citizen. Best-effort: {@code lifecareChecked} is
 * false
 * when Lifecare could not be reached, in which case {@code children} is empty.
 */
@Schema(description = "Pre-fill data for a financial assistance renewal (renewal): household children read from Lifecare.")
public class RenewalPrefill {

	@ArraySchema(arraySchema = @Schema(description = "Children in the household from the most recent calculation"), schema = @Schema(implementation = PrefilledChild.class))
	private List<PrefilledChild> children;

	@Schema(description = "True when the Lifecare lookup succeeded. False means the answer is degraded (empty children).", examples = "true")
	private boolean lifecareChecked;

	public static RenewalPrefill create() {
		return new RenewalPrefill();
	}

	public List<PrefilledChild> getChildren() {
		return children;
	}

	public void setChildren(final List<PrefilledChild> children) {
		this.children = children;
	}

	public RenewalPrefill withChildren(final List<PrefilledChild> children) {
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
		return lifecareChecked == that.lifecareChecked && Objects.equals(children, that.children);
	}

	@Override
	public int hashCode() {
		return Objects.hash(children, lifecareChecked);
	}

	@Override
	public String toString() {
		return "RenewalPrefill{children=" + children + ", lifecareChecked=" + lifecareChecked + '}';
	}
}
