package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "A document type a caseworker can pick for a new document.")
public class LifecareDocumentType {

	@Schema(description = "Lifecare's documentTypeCode", examples = "1")
	private Integer code;

	@Schema(description = "The document type's name", examples = "EK Brev")
	private String name;

	@Schema(description = "Whether the documented date may differ from the one Lifecare proposes (today)", examples = "true")
	private Boolean canChangeOccurenceDate;

	@Schema(description = "Whether a new document of this type is saved skrivskyddad unless the caseworker says otherwise", examples = "false")
	private Boolean protectedByDefault;

	public static LifecareDocumentType create() {
		return new LifecareDocumentType();
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(final Integer code) {
		this.code = code;
	}

	public LifecareDocumentType withCode(final Integer code) {
		this.code = code;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public LifecareDocumentType withName(final String name) {
		this.name = name;
		return this;
	}

	public Boolean getCanChangeOccurenceDate() {
		return canChangeOccurenceDate;
	}

	public void setCanChangeOccurenceDate(final Boolean canChangeOccurenceDate) {
		this.canChangeOccurenceDate = canChangeOccurenceDate;
	}

	public LifecareDocumentType withCanChangeOccurenceDate(final Boolean canChangeOccurenceDate) {
		this.canChangeOccurenceDate = canChangeOccurenceDate;
		return this;
	}

	public Boolean getProtectedByDefault() {
		return protectedByDefault;
	}

	public void setProtectedByDefault(final Boolean protectedByDefault) {
		this.protectedByDefault = protectedByDefault;
	}

	public LifecareDocumentType withProtectedByDefault(final Boolean protectedByDefault) {
		this.protectedByDefault = protectedByDefault;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final LifecareDocumentType that = (LifecareDocumentType) o;
		return Objects.equals(code, that.code) && Objects.equals(name, that.name) && Objects.equals(canChangeOccurenceDate, that.canChangeOccurenceDate) && Objects.equals(protectedByDefault, that.protectedByDefault);
	}

	@Override
	public int hashCode() {
		return Objects.hash(code, name, canChangeOccurenceDate, protectedByDefault);
	}

	@Override
	public String toString() {
		return "LifecareDocumentType{" + "code=" + code + ", name='" + name + '\'' + ", canChangeOccurenceDate=" + canChangeOccurenceDate + ", protectedByDefault=" + protectedByDefault + '}';
	}
}
