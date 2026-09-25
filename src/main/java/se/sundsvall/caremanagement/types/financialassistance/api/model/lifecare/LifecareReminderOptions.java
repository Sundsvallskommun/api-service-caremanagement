package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "The priorities and statuses a bevakning can have, as Lifecare lists them, and what Lifecare proposes for a new one.")
public class LifecareReminderOptions {

	@Schema(description = "The active priorities")
	private List<LifecareReminderChoice> priorities;

	@Schema(description = "The active statuses")
	private List<LifecareReminderChoice> statuses;

	@Schema(description = "The priority Lifecare proposes for a new bevakning", examples = "2")
	private Integer defaultPriority;

	@Schema(description = "The status Lifecare proposes for a new bevakning", examples = "3")
	private Integer defaultStatus;

	public static LifecareReminderOptions create() {
		return new LifecareReminderOptions();
	}

	public List<LifecareReminderChoice> getPriorities() {
		return priorities;
	}

	public void setPriorities(final List<LifecareReminderChoice> priorities) {
		this.priorities = priorities;
	}

	public LifecareReminderOptions withPriorities(final List<LifecareReminderChoice> priorities) {
		this.priorities = priorities;
		return this;
	}

	public List<LifecareReminderChoice> getStatuses() {
		return statuses;
	}

	public void setStatuses(final List<LifecareReminderChoice> statuses) {
		this.statuses = statuses;
	}

	public LifecareReminderOptions withStatuses(final List<LifecareReminderChoice> statuses) {
		this.statuses = statuses;
		return this;
	}

	public Integer getDefaultPriority() {
		return defaultPriority;
	}

	public void setDefaultPriority(final Integer defaultPriority) {
		this.defaultPriority = defaultPriority;
	}

	public LifecareReminderOptions withDefaultPriority(final Integer defaultPriority) {
		this.defaultPriority = defaultPriority;
		return this;
	}

	public Integer getDefaultStatus() {
		return defaultStatus;
	}

	public void setDefaultStatus(final Integer defaultStatus) {
		this.defaultStatus = defaultStatus;
	}

	public LifecareReminderOptions withDefaultStatus(final Integer defaultStatus) {
		this.defaultStatus = defaultStatus;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final LifecareReminderOptions that = (LifecareReminderOptions) o;
		return Objects.equals(priorities, that.priorities) && Objects.equals(statuses, that.statuses) && Objects.equals(defaultPriority, that.defaultPriority) && Objects.equals(defaultStatus, that.defaultStatus);
	}

	@Override
	public int hashCode() {
		return Objects.hash(priorities, statuses, defaultPriority, defaultStatus);
	}

	@Override
	public String toString() {
		return "LifecareReminderOptions{" + "priorities=" + priorities + ", statuses=" + statuses + ", defaultPriority=" + defaultPriority + ", defaultStatus=" + defaultStatus + '}';
	}
}
