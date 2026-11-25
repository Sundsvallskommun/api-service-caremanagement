package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Objects;

@Schema(description = "The edits a caseworker makes to a Lifecare journalanteckning or document. Everything else on the Lifecare record is kept as Lifecare has it.")
public class UpdateLifecareRecordRequest {

	@Schema(description = "The record body as HTML", examples = "<p>Hej</p>", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Size(max = 1048576)
	private String content;

	@Schema(description = "Documented date, YYYY-MM-DD", examples = "2026-09-23")
	@Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "must be YYYY-MM-DD")
	private String occurenceDate;

	@Schema(description = "Documented time, HH:mm", examples = "09:02")
	@Pattern(regexp = "^\\d{2}:\\d{2}$", message = "must be HH:mm")
	private String time;

	@Schema(name = "protected", description = "Write-protects the record with this save; Lifecare then allows no further change", examples = "false")
	@JsonProperty("protected")
	private Boolean writeProtected;

	public static UpdateLifecareRecordRequest create() {
		return new UpdateLifecareRecordRequest();
	}

	public String getContent() {
		return content;
	}

	public void setContent(final String content) {
		this.content = content;
	}

	public UpdateLifecareRecordRequest withContent(final String content) {
		this.content = content;
		return this;
	}

	public String getOccurenceDate() {
		return occurenceDate;
	}

	public void setOccurenceDate(final String occurenceDate) {
		this.occurenceDate = occurenceDate;
	}

	public UpdateLifecareRecordRequest withOccurenceDate(final String occurenceDate) {
		this.occurenceDate = occurenceDate;
		return this;
	}

	public String getTime() {
		return time;
	}

	public void setTime(final String time) {
		this.time = time;
	}

	public UpdateLifecareRecordRequest withTime(final String time) {
		this.time = time;
		return this;
	}

	public Boolean getWriteProtected() {
		return writeProtected;
	}

	public void setWriteProtected(final Boolean writeProtected) {
		this.writeProtected = writeProtected;
	}

	public UpdateLifecareRecordRequest withWriteProtected(final Boolean writeProtected) {
		this.writeProtected = writeProtected;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final UpdateLifecareRecordRequest that = (UpdateLifecareRecordRequest) o;
		return Objects.equals(content, that.content) && Objects.equals(occurenceDate, that.occurenceDate) && Objects.equals(time, that.time) && Objects.equals(writeProtected, that.writeProtected);
	}

	@Override
	public int hashCode() {
		return Objects.hash(content, occurenceDate, time, writeProtected);
	}

	@Override
	public String toString() {
		return "UpdateLifecareRecordRequest{" + "content='" + content + '\'' + ", occurenceDate='" + occurenceDate + '\'' + ", time='" + time + '\'' + ", writeProtected=" + writeProtected + '}';
	}
}
