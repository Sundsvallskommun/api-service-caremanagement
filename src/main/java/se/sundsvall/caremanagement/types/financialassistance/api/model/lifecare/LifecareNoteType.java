package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "A note type a caseworker can pick for a new journalanteckning.")
public class LifecareNoteType {

	@Schema(description = "Lifecare's noteTypeCode", examples = "1")
	private Integer code;

	@Schema(description = "The note type's name", examples = "Journalanteckning")
	private String name;

	@Schema(description = "Whether a new note of this type is saved skrivskyddad unless the caseworker says otherwise", examples = "false")
	private Boolean protectedByDefault;

	public static LifecareNoteType create() {
		return new LifecareNoteType();
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(final Integer code) {
		this.code = code;
	}

	public LifecareNoteType withCode(final Integer code) {
		this.code = code;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public LifecareNoteType withName(final String name) {
		this.name = name;
		return this;
	}

	public Boolean getProtectedByDefault() {
		return protectedByDefault;
	}

	public void setProtectedByDefault(final Boolean protectedByDefault) {
		this.protectedByDefault = protectedByDefault;
	}

	public LifecareNoteType withProtectedByDefault(final Boolean protectedByDefault) {
		this.protectedByDefault = protectedByDefault;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final LifecareNoteType that = (LifecareNoteType) o;
		return Objects.equals(code, that.code) && Objects.equals(name, that.name) && Objects.equals(protectedByDefault, that.protectedByDefault);
	}

	@Override
	public int hashCode() {
		return Objects.hash(code, name, protectedByDefault);
	}

	@Override
	public String toString() {
		return "LifecareNoteType{" + "code=" + code + ", name='" + name + '\'' + ", protectedByDefault=" + protectedByDefault + '}';
	}
}
