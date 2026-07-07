package se.sundsvall.caremanagement.document.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/**
 * The document metadata response — the catalogue of selectable document types the frontend offers in the "Typ"
 * dropdown.
 * Document types are municipality-configured in Lifecare; this set is seeded {@code DOCUMENT_TYPE} lookups for the
 * namespace (or a built-in provisional fallback) and is not enforced on {@code Document.type}.
 */
@Schema(description = "Document metadata — the catalogue of selectable document types.")
public class DocumentMetadata {

	@ArraySchema(arraySchema = @Schema(description = "Selectable document types"), schema = @Schema(implementation = DocumentType.class))
	private List<DocumentType> types;

	public static DocumentMetadata create() {
		return new DocumentMetadata();
	}

	public List<DocumentType> getTypes() {
		return types;
	}

	public void setTypes(final List<DocumentType> types) {
		this.types = types;
	}

	public DocumentMetadata withTypes(final List<DocumentType> types) {
		this.types = types;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final DocumentMetadata that = (DocumentMetadata) o;
		return Objects.equals(types, that.types);
	}

	@Override
	public int hashCode() {
		return Objects.hash(types);
	}
}
