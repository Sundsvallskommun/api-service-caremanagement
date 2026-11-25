package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "A Lifecare record with its body, as shown when a record is opened.")
public class LifecareRecordContent {

	@Schema(description = "The Lifecare record id", examples = "135")
	private String id;

	@Schema(description = "Which group the record belongs to", examples = "JOURNAL_NOTE", allowableValues = {
		"JOURNAL_NOTE", "DOCUMENT"
	})
	private String category;

	@Schema(description = "The rubrik", examples = "Journalanteckning")
	private String title;

	@Schema(description = "The body as HTML", examples = "<p>Hej</p>")
	private String content;

	@Schema(description = "Documented date, YYYY-MM-DD", examples = "2026-09-23")
	private String occurenceDate;

	@Schema(description = "Documented time, HH:mm (empty when the record has none)", examples = "09:02")
	private String time;

	@Schema(description = "Whether the record may still be edited in Lifecare", examples = "true")
	private Boolean editable;

	public static LifecareRecordContent create() {
		return new LifecareRecordContent();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public LifecareRecordContent withId(final String id) {
		this.id = id;
		return this;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(final String category) {
		this.category = category;
	}

	public LifecareRecordContent withCategory(final String category) {
		this.category = category;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public LifecareRecordContent withTitle(final String title) {
		this.title = title;
		return this;
	}

	public String getContent() {
		return content;
	}

	public void setContent(final String content) {
		this.content = content;
	}

	public LifecareRecordContent withContent(final String content) {
		this.content = content;
		return this;
	}

	public String getOccurenceDate() {
		return occurenceDate;
	}

	public void setOccurenceDate(final String occurenceDate) {
		this.occurenceDate = occurenceDate;
	}

	public LifecareRecordContent withOccurenceDate(final String occurenceDate) {
		this.occurenceDate = occurenceDate;
		return this;
	}

	public String getTime() {
		return time;
	}

	public void setTime(final String time) {
		this.time = time;
	}

	public LifecareRecordContent withTime(final String time) {
		this.time = time;
		return this;
	}

	public Boolean getEditable() {
		return editable;
	}

	public void setEditable(final Boolean editable) {
		this.editable = editable;
	}

	public LifecareRecordContent withEditable(final Boolean editable) {
		this.editable = editable;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final LifecareRecordContent that = (LifecareRecordContent) o;
		return Objects.equals(id, that.id) && Objects.equals(category, that.category) && Objects.equals(title, that.title) && Objects.equals(content, that.content) && Objects.equals(occurenceDate, that.occurenceDate) && Objects.equals(time, that.time)
			&& Objects.equals(editable, that.editable);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, category, title, content, occurenceDate, time, editable);
	}

	@Override
	public String toString() {
		return "LifecareRecordContent{" + "id='" + id + '\'' + ", category='" + category + '\'' + ", title='" + title + '\'' + ", content='" + content + '\'' + ", occurenceDate='" + occurenceDate + '\'' + ", time='" + time + '\'' + ", editable=" + editable
			+ '}';
	}
}
