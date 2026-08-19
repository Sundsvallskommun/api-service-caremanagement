package se.sundsvall.caremanagement.document.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * One selectable document type — a machine {@code code} paired with the Swedish Lifecare label the frontend shows and
 * that an RPA flow selects in the Lifecare "Typ" dropdown. Document types (Dokumenttyp) are municipality-configured in
 * Lifecare, so the served catalogue is provisional and {@code Document.type} is not validated against it.
 */
@Schema(description = "A selectable document type — the code and the Swedish Lifecare label.")
public class DocumentType {

	@Schema(description = "The type code", examples = "LETTER")
	private String code;

	@Schema(description = "Human-readable Swedish label (the Lifecare 'Typ' value)", examples = "Brev")
	private String displayName;

	public static DocumentType create() {
		return new DocumentType();
	}

	public String getCode() {
		return code;
	}

	public void setCode(final String code) {
		this.code = code;
	}

	public DocumentType withCode(final String code) {
		this.code = code;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public DocumentType withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final DocumentType that = (DocumentType) o;
		return Objects.equals(code, that.code) && Objects.equals(displayName, that.displayName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(code, displayName);
	}

	@Override
	public String toString() {
		return "DocumentType{code='" + code + "', displayName='" + displayName + "'}";
	}
}
