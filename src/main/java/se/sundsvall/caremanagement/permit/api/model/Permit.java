package se.sundsvall.caremanagement.permit.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import se.sundsvall.caremanagement.core.api.validation.groups.OnCreate;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

/**
 * An issued permit on an errand — the structured validity period, conditions and status that the flat {@code Decision}
 * cannot hold. Type-agnostic: {@code permitType} is namespace-defined free text. {@code validFrom} defaults to today
 * when omitted; {@code validUntil} is open-ended unless supplied. {@code status} moves ACTIVE → REVOKED on revocation.
 */
@Schema(description = "An issued permit with a validity period, conditions and status.")
public class Permit {

	@Schema(description = "Unique id", examples = "cb20c51f-fcf3-42c0-b613-de563634a8ec", accessMode = READ_ONLY)
	@Null(groups = OnCreate.class)
	private String id;

	@Schema(description = "The type of permit (namespace-defined)", examples = "PARKING_PERMIT")
	@NotBlank(groups = OnCreate.class)
	@Size(max = 64)
	private String permitType;

	@Schema(description = "Valid from (decision date). Defaults to today when omitted.", examples = "2026-06-03")
	@DateTimeFormat(iso = DATE)
	private LocalDate validFrom;

	@Schema(description = "Valid until and including. Open-ended when omitted.", examples = "2031-09-01")
	@DateTimeFormat(iso = DATE)
	private LocalDate validUntil;

	@Schema(description = "Conditions for the permit", examples = "Unloading may only take place during daytime.")
	@Size(max = 4096)
	private String conditions;

	@Schema(description = "Status", examples = "ACTIVE", allowableValues = {
		"ACTIVE", "REVOKED"
	})
	@OneOf(value = {
		"ACTIVE", "REVOKED"
	}, nullable = true)
	private String status;

	@Schema(description = "Created", accessMode = READ_ONLY)
	@Null(groups = OnCreate.class)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime created;

	@Schema(description = "Modified", accessMode = READ_ONLY)
	@Null(groups = OnCreate.class)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime modified;

	public static Permit create() {
		return new Permit();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Permit withId(final String id) {
		this.id = id;
		return this;
	}

	public String getPermitType() {
		return permitType;
	}

	public void setPermitType(final String permitType) {
		this.permitType = permitType;
	}

	public Permit withPermitType(final String permitType) {
		this.permitType = permitType;
		return this;
	}

	public LocalDate getValidFrom() {
		return validFrom;
	}

	public void setValidFrom(final LocalDate validFrom) {
		this.validFrom = validFrom;
	}

	public Permit withValidFrom(final LocalDate validFrom) {
		this.validFrom = validFrom;
		return this;
	}

	public LocalDate getValidUntil() {
		return validUntil;
	}

	public void setValidUntil(final LocalDate validUntil) {
		this.validUntil = validUntil;
	}

	public Permit withValidUntil(final LocalDate validUntil) {
		this.validUntil = validUntil;
		return this;
	}

	public String getConditions() {
		return conditions;
	}

	public void setConditions(final String conditions) {
		this.conditions = conditions;
	}

	public Permit withConditions(final String conditions) {
		this.conditions = conditions;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public Permit withStatus(final String status) {
		this.status = status;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Permit withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public Permit withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Permit that = (Permit) o;
		return Objects.equals(id, that.id) && Objects.equals(permitType, that.permitType)
			&& Objects.equals(validFrom, that.validFrom) && Objects.equals(validUntil, that.validUntil)
			&& Objects.equals(conditions, that.conditions) && Objects.equals(status, that.status)
			&& Objects.equals(created, that.created) && Objects.equals(modified, that.modified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, permitType, validFrom, validUntil, conditions, status, created, modified);
	}

	@Override
	public String toString() {
		return "Permit{id='" + id + "', permitType='" + permitType + "', validFrom=" + validFrom
			+ ", validUntil=" + validUntil + ", conditions='" + conditions + "', status='" + status
			+ "', created=" + created + ", modified=" + modified + '}';
	}
}
