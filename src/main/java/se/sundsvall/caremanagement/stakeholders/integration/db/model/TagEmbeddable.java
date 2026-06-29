package se.sundsvall.caremanagement.stakeholders.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Generic key/value tag — used for {@link StakeholderEntity#getContactChannels() contact channels
 * on stakeholders}. The errand-level {@code externalTags} usage was killed in Phase 2a (D3 in
 * docs/architecture/proposal-modulith.md).
 *
 * The columns are the quoted reserved words {@code "key"} and {@code "value"}, fixed on the embeddable
 * itself — there is a single use-site (the contact-channel element collection) and no
 * {@code @AttributeOverride}.
 */
@Embeddable
public class TagEmbeddable {

	@Column(name = "\"key\"")
	private String key;

	@Column(name = "\"value\"")
	private String value;

	public static TagEmbeddable create() {
		return new TagEmbeddable();
	}

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	public TagEmbeddable withKey(final String key) {
		this.key = key;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public TagEmbeddable withValue(final String value) {
		this.value = value;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, value);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final var that = (TagEmbeddable) o;
		return Objects.equals(key, that.key) && Objects.equals(value, that.value);
	}

	@Override
	public String toString() {
		return "TagEmbeddable{" +
			"key='" + key + '\'' +
			", value='" + value + '\'' +
			'}';
	}
}
