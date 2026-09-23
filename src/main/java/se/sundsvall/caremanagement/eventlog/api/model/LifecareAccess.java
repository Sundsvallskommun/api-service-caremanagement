package se.sundsvall.caremanagement.eventlog.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

/**
 * One read or write a caller made in Lifecare directly, on an errand's behalf, reported so that it lands in the
 * errand's access log. Draken's BFF reads journal, documents, reminders and jobbstimulans live from Lifecare and writes
 * journal notes, documents and reminders straight into it; none of that passes careM's own request logging, and
 * verksamhetens regelverk (revision 2026-09-22) requires every access to be logged on the errand and the user.
 *
 * <p>
 * Deliberately no free-form Lifecare path: Lifecare's paths and queries carry personal numbers, and the access log
 * must not become a second place they are stored. The caller names <em>what</em> it accessed ({@code target}) and,
 * where there is one, the Lifecare record id.
 * </p>
 */
@Schema(description = "One read or write made in Lifecare directly on an errand's behalf, reported to the errand's access log.")
public class LifecareAccess {

	@Schema(description = "What was done in Lifecare", examples = "READ", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {
		"READ", "CREATE", "UPDATE", "DELETE"
	})
	@NotBlank
	@OneOf({
		"READ", "CREATE", "UPDATE", "DELETE"
	})
	private String action;

	@Schema(description = "What in Lifecare was accessed — a stable name, not a Lifecare path",
		examples = "lifecare/journal-notes",
		requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	@Size(max = 255)
	private String target;

	@Schema(description = "A human-readable summary, shown in the access log", examples = "Läste journalen i Lifecare")
	@Size(max = 512)
	private String description;

	@Schema(description = "The Lifecare record the access was about (e.g. the journal note written), when there is one", examples = "4711")
	@Size(max = 64)
	private String lifecareId;

	public static LifecareAccess create() {
		return new LifecareAccess();
	}

	public String getAction() {
		return action;
	}

	public void setAction(final String action) {
		this.action = action;
	}

	public LifecareAccess withAction(final String action) {
		this.action = action;
		return this;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(final String target) {
		this.target = target;
	}

	public LifecareAccess withTarget(final String target) {
		this.target = target;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public LifecareAccess withDescription(final String description) {
		this.description = description;
		return this;
	}

	public String getLifecareId() {
		return lifecareId;
	}

	public void setLifecareId(final String lifecareId) {
		this.lifecareId = lifecareId;
	}

	public LifecareAccess withLifecareId(final String lifecareId) {
		this.lifecareId = lifecareId;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final LifecareAccess that = (LifecareAccess) o;
		return Objects.equals(action, that.action) && Objects.equals(target, that.target) && Objects.equals(description, that.description)
			&& Objects.equals(lifecareId, that.lifecareId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(action, target, description, lifecareId);
	}

	@Override
	public String toString() {
		return "LifecareAccess{" +
			"action='" + action + '\'' +
			", target='" + target + '\'' +
			", description='" + description + '\'' +
			", lifecareId='" + lifecareId + '\'' +
			'}';
	}
}
