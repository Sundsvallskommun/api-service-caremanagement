package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "A coded choice in the bevakning form.")
public class LifecareReminderChoice {

	@Schema(description = "Lifecare's code", examples = "2")
	private Integer code;

	@Schema(description = "The text Lifecare gives the code", examples = "Normal")
	private String text;

	public static LifecareReminderChoice create() {
		return new LifecareReminderChoice();
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(final Integer code) {
		this.code = code;
	}

	public LifecareReminderChoice withCode(final Integer code) {
		this.code = code;
		return this;
	}

	public String getText() {
		return text;
	}

	public void setText(final String text) {
		this.text = text;
	}

	public LifecareReminderChoice withText(final String text) {
		this.text = text;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final LifecareReminderChoice that = (LifecareReminderChoice) o;
		return Objects.equals(code, that.code) && Objects.equals(text, that.text);
	}

	@Override
	public int hashCode() {
		return Objects.hash(code, text);
	}

	@Override
	public String toString() {
		return "LifecareReminderChoice{" + "code=" + code + ", text='" + text + '\'' + '}';
	}
}
