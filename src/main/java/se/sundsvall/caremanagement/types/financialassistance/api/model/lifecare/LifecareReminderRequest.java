package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Objects;

@Schema(description = "A bevakning as the caseworker fills it in, new or changed. It is always bevakad av the insats's caseworker in Lifecare.")
public class LifecareReminderRequest {

	@Schema(description = "Bevakningsdatum, YYYY-MM-DD. Not before today (Swedish time); on a change only checked when the date changes.", examples = "2026-09-30", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "must be YYYY-MM-DD")
	private String reminderDate;

	@Schema(description = "The bevakning text", examples = "Kontrollera hyran", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	@Size(max = 4000)
	private String text;

	@Schema(description = "Lifecare's priority code, from the options endpoint", examples = "2", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private Integer priority;

	@Schema(description = "Lifecare's status code, from the options endpoint", examples = "3", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private Integer status;

	public static LifecareReminderRequest create() {
		return new LifecareReminderRequest();
	}

	public String getReminderDate() {
		return reminderDate;
	}

	public void setReminderDate(final String reminderDate) {
		this.reminderDate = reminderDate;
	}

	public LifecareReminderRequest withReminderDate(final String reminderDate) {
		this.reminderDate = reminderDate;
		return this;
	}

	public String getText() {
		return text;
	}

	public void setText(final String text) {
		this.text = text;
	}

	public LifecareReminderRequest withText(final String text) {
		this.text = text;
		return this;
	}

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(final Integer priority) {
		this.priority = priority;
	}

	public LifecareReminderRequest withPriority(final Integer priority) {
		this.priority = priority;
		return this;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(final Integer status) {
		this.status = status;
	}

	public LifecareReminderRequest withStatus(final Integer status) {
		this.status = status;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final LifecareReminderRequest that = (LifecareReminderRequest) o;
		return Objects.equals(reminderDate, that.reminderDate) && Objects.equals(text, that.text) && Objects.equals(priority, that.priority) && Objects.equals(status, that.status);
	}

	@Override
	public int hashCode() {
		return Objects.hash(reminderDate, text, priority, status);
	}

	@Override
	public String toString() {
		return "LifecareReminderRequest{" + "reminderDate='" + reminderDate + '\'' + ", text='" + text + '\'' + ", priority=" + priority + ", status=" + status + '}';
	}
}
