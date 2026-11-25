package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "A record's body, shown in the list without opening the record.")
public class LifecareRecordBody {

	@Schema(description = "The Lifecare record id", examples = "135")
	private String id;

	@Schema(description = "The body as HTML; left out when Lifecare would not hand it over", examples = "<p>Hej</p>")
	private String content;

	public static LifecareRecordBody create() {
		return new LifecareRecordBody();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public LifecareRecordBody withId(final String id) {
		this.id = id;
		return this;
	}

	public String getContent() {
		return content;
	}

	public void setContent(final String content) {
		this.content = content;
	}

	public LifecareRecordBody withContent(final String content) {
		this.content = content;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final LifecareRecordBody that = (LifecareRecordBody) o;
		return Objects.equals(id, that.id) && Objects.equals(content, that.content);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, content);
	}

	@Override
	public String toString() {
		return "LifecareRecordBody{" + "id='" + id + '\'' + ", content='" + content + '\'' + '}';
	}
}
