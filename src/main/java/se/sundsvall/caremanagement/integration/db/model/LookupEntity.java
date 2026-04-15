package se.sundsvall.caremanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;

import static jakarta.persistence.GenerationType.IDENTITY;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * Generic namespace-scoped, municipality-scoped named reference value. The {@code kind} discriminator separates what
 * would otherwise have been five near-identical tables (category/status/type/role/contact_reason) into a single table
 * with one entity.
 * Typed service facades live above this class.
 */
@Entity
@Table(name = "lookup",
	indexes = {
		@Index(name = "idx_lookup_kind_namespace_municipality_id", columnList = "kind, namespace, municipality_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_lookup_kind_namespace_municipality_id_name", columnNames = {
			"kind", "namespace", "municipality_id", "name"
		})
	})
@EntityListeners(AuditableListener.class)
public class LookupEntity implements Auditable {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "kind", nullable = false, length = 32)
	private LookupKind kind;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "display_name")
	private String displayName;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 32)
	private String namespace;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	public static LookupEntity create() {
		return new LookupEntity();
	}

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public LookupEntity withId(final Long id) {
		this.id = id;
		return this;
	}

	public LookupKind getKind() {
		return kind;
	}

	public void setKind(final LookupKind kind) {
		this.kind = kind;
	}

	public LookupEntity withKind(final LookupKind kind) {
		this.kind = kind;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public LookupEntity withName(final String name) {
		this.name = name;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public LookupEntity withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public LookupEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public LookupEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	@Override
	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public LookupEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	@Override
	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public LookupEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final LookupEntity that = (LookupEntity) o;
		return Objects.equals(id, that.id) && kind == that.kind && Objects.equals(name, that.name) && Objects.equals(displayName, that.displayName)
			&& Objects.equals(municipalityId, that.municipalityId) && Objects.equals(namespace, that.namespace) && Objects.equals(created, that.created) && Objects.equals(modified, that.modified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, kind, name, displayName, municipalityId, namespace, created, modified);
	}

	@Override
	public String toString() {
		return "LookupEntity{" +
			"id=" + id +
			", kind=" + kind +
			", name='" + name + '\'' +
			", displayName='" + displayName + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", namespace='" + namespace + '\'' +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
