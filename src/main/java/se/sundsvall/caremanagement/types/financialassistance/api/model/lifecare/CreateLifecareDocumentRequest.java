package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Objects;

@Schema(description = "A new document, written straight to the errand's insats in Lifecare.")
public class CreateLifecareDocumentRequest {

	@Schema(description = "The document body as HTML", examples = "<p>Hej</p>", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	@Size(max = 1048576)
	private String content;

	@Schema(description = "Lifecare's documentTypeCode, from the document-types endpoint", examples = "1", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private Integer documentTypeCode;

	@Schema(description = "The rubrik; the document type's name when left out", examples = "Beslut om bistånd")
	@Size(max = 255)
	private String title;

	@Schema(description = "Documented date, YYYY-MM-DD; Lifecare's proposal (today) when left out or when the type fixes it", examples = "2026-09-23")
	@Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "must be YYYY-MM-DD")
	private String occurenceDate;

	@Schema(name = "protected", description = "Saved skrivskyddad, after which it can no longer be changed. The document type's own default when left out.", examples = "false")
	@JsonProperty("protected")
	private Boolean writeProtected;

	public static CreateLifecareDocumentRequest create() {
		return new CreateLifecareDocumentRequest();
	}

	public String getContent() {
		return content;
	}

	public void setContent(final String content) {
		this.content = content;
	}

	public CreateLifecareDocumentRequest withContent(final String content) {
		this.content = content;
		return this;
	}

	public Integer getDocumentTypeCode() {
		return documentTypeCode;
	}

	public void setDocumentTypeCode(final Integer documentTypeCode) {
		this.documentTypeCode = documentTypeCode;
	}

	public CreateLifecareDocumentRequest withDocumentTypeCode(final Integer documentTypeCode) {
		this.documentTypeCode = documentTypeCode;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public CreateLifecareDocumentRequest withTitle(final String title) {
		this.title = title;
		return this;
	}

	public String getOccurenceDate() {
		return occurenceDate;
	}

	public void setOccurenceDate(final String occurenceDate) {
		this.occurenceDate = occurenceDate;
	}

	public CreateLifecareDocumentRequest withOccurenceDate(final String occurenceDate) {
		this.occurenceDate = occurenceDate;
		return this;
	}

	public Boolean getWriteProtected() {
		return writeProtected;
	}

	public void setWriteProtected(final Boolean writeProtected) {
		this.writeProtected = writeProtected;
	}

	public CreateLifecareDocumentRequest withWriteProtected(final Boolean writeProtected) {
		this.writeProtected = writeProtected;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final CreateLifecareDocumentRequest that = (CreateLifecareDocumentRequest) o;
		return Objects.equals(content, that.content) && Objects.equals(documentTypeCode, that.documentTypeCode) && Objects.equals(title, that.title) && Objects.equals(occurenceDate, that.occurenceDate) && Objects.equals(writeProtected,
			that.writeProtected);
	}

	@Override
	public int hashCode() {
		return Objects.hash(content, documentTypeCode, title, occurenceDate, writeProtected);
	}

	@Override
	public String toString() {
		return "CreateLifecareDocumentRequest{" + "content='" + content + '\'' + ", documentTypeCode=" + documentTypeCode + ", title='" + title + '\'' + ", occurenceDate='" + occurenceDate + '\'' + ", writeProtected=" + writeProtected + '}';
	}
}
