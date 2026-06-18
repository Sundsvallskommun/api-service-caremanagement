package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

/**
 * One person row of the normberäkning draft, as returned to Draken — a household member (applicant, co-applicant or
 * child). {@code processDays} is the number of days in the home the process derived (read-only); the handläggare's
 * override ({@code handlaggareDays}) and the note are editable. {@code effectiveDays} is what is used = the handläggare
 * value when set, otherwise the process value. Drives the norm base.
 */
@Schema(description = "One person row of the normberäkning draft (household member, process vs handläggare days).")
public class NormPersonRow {

	@Schema(description = "The row id", accessMode = Schema.AccessMode.READ_ONLY)
	private String id;

	@Schema(description = "Who created the row: the process or a handläggare", allowableValues = {
		"SYSTEM", "HANDLAGGARE"
	}, accessMode = Schema.AccessMode.READ_ONLY)
	private String origin;

	@Schema(description = "The party id of the household member", accessMode = Schema.AccessMode.READ_ONLY)
	private String partyId;

	@Schema(description = "The role of the household member", allowableValues = {
		"APPLICANT", "CO_APPLICANT", "CHILD"
	}, accessMode = Schema.AccessMode.READ_ONLY)
	private String role;

	@Schema(description = "The name of the household member", accessMode = Schema.AccessMode.READ_ONLY)
	private String name;

	@Schema(description = "The number of days in the home the process derived", examples = "30", accessMode = Schema.AccessMode.READ_ONLY)
	private Integer processDays;

	@Schema(description = "The number of days a handläggare decided; overrides the process value when set", examples = "15")
	private Integer handlaggareDays;

	@Schema(description = "The number of days actually used (handläggare value when set, otherwise process value)", accessMode = Schema.AccessMode.READ_ONLY)
	private Integer effectiveDays;

	@Schema(description = "Whether the household member is included in the norm")
	private boolean included;

	@Schema(description = "The start date of the member's deviation from the household")
	@DateTimeFormat(iso = DATE)
	private LocalDate deviationFromDate;

	@Schema(description = "The end date of the member's deviation from the household")
	@DateTimeFormat(iso = DATE)
	private LocalDate deviationToDate;

	@Schema(description = "The norm interval applied to the member")
	private String normInterval;

	@Schema(description = "The jobbstimulans amount applied to the member", examples = "1000.00")
	private BigDecimal jobbstimulansAmount;

	@Schema(description = "Whether the row is soft-deleted (excluded from the calculation, not resurrected by the daily refresh)", accessMode = Schema.AccessMode.READ_ONLY)
	private boolean deleted;

	@Schema(description = "Free-text note")
	private String note;

	@Schema(description = "When the row was created", accessMode = Schema.AccessMode.READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime created;

	@Schema(description = "When the row was last updated", accessMode = Schema.AccessMode.READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime updated;

	public static NormPersonRow create() {
		return new NormPersonRow();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public NormPersonRow withId(final String id) {
		this.id = id;
		return this;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(final String origin) {
		this.origin = origin;
	}

	public NormPersonRow withOrigin(final String origin) {
		this.origin = origin;
		return this;
	}

	public String getPartyId() {
		return partyId;
	}

	public void setPartyId(final String partyId) {
		this.partyId = partyId;
	}

	public NormPersonRow withPartyId(final String partyId) {
		this.partyId = partyId;
		return this;
	}

	public String getRole() {
		return role;
	}

	public void setRole(final String role) {
		this.role = role;
	}

	public NormPersonRow withRole(final String role) {
		this.role = role;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public NormPersonRow withName(final String name) {
		this.name = name;
		return this;
	}

	public Integer getProcessDays() {
		return processDays;
	}

	public void setProcessDays(final Integer processDays) {
		this.processDays = processDays;
	}

	public NormPersonRow withProcessDays(final Integer processDays) {
		this.processDays = processDays;
		return this;
	}

	public Integer getHandlaggareDays() {
		return handlaggareDays;
	}

	public void setHandlaggareDays(final Integer handlaggareDays) {
		this.handlaggareDays = handlaggareDays;
	}

	public NormPersonRow withHandlaggareDays(final Integer handlaggareDays) {
		this.handlaggareDays = handlaggareDays;
		return this;
	}

	public Integer getEffectiveDays() {
		return effectiveDays;
	}

	public void setEffectiveDays(final Integer effectiveDays) {
		this.effectiveDays = effectiveDays;
	}

	public NormPersonRow withEffectiveDays(final Integer effectiveDays) {
		this.effectiveDays = effectiveDays;
		return this;
	}

	public boolean isIncluded() {
		return included;
	}

	public void setIncluded(final boolean included) {
		this.included = included;
	}

	public NormPersonRow withIncluded(final boolean included) {
		this.included = included;
		return this;
	}

	public LocalDate getDeviationFromDate() {
		return deviationFromDate;
	}

	public void setDeviationFromDate(final LocalDate deviationFromDate) {
		this.deviationFromDate = deviationFromDate;
	}

	public NormPersonRow withDeviationFromDate(final LocalDate deviationFromDate) {
		this.deviationFromDate = deviationFromDate;
		return this;
	}

	public LocalDate getDeviationToDate() {
		return deviationToDate;
	}

	public void setDeviationToDate(final LocalDate deviationToDate) {
		this.deviationToDate = deviationToDate;
	}

	public NormPersonRow withDeviationToDate(final LocalDate deviationToDate) {
		this.deviationToDate = deviationToDate;
		return this;
	}

	public String getNormInterval() {
		return normInterval;
	}

	public void setNormInterval(final String normInterval) {
		this.normInterval = normInterval;
	}

	public NormPersonRow withNormInterval(final String normInterval) {
		this.normInterval = normInterval;
		return this;
	}

	public BigDecimal getJobbstimulansAmount() {
		return jobbstimulansAmount;
	}

	public void setJobbstimulansAmount(final BigDecimal jobbstimulansAmount) {
		this.jobbstimulansAmount = jobbstimulansAmount;
	}

	public NormPersonRow withJobbstimulansAmount(final BigDecimal jobbstimulansAmount) {
		this.jobbstimulansAmount = jobbstimulansAmount;
		return this;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(final boolean deleted) {
		this.deleted = deleted;
	}

	public NormPersonRow withDeleted(final boolean deleted) {
		this.deleted = deleted;
		return this;
	}

	public String getNote() {
		return note;
	}

	public void setNote(final String note) {
		this.note = note;
	}

	public NormPersonRow withNote(final String note) {
		this.note = note;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public NormPersonRow withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getUpdated() {
		return updated;
	}

	public void setUpdated(final OffsetDateTime updated) {
		this.updated = updated;
	}

	public NormPersonRow withUpdated(final OffsetDateTime updated) {
		this.updated = updated;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final NormPersonRow that = (NormPersonRow) o;
		return deleted == that.deleted && included == that.included && Objects.equals(id, that.id) && Objects.equals(origin, that.origin)
			&& Objects.equals(partyId, that.partyId) && Objects.equals(role, that.role) && Objects.equals(name, that.name)
			&& Objects.equals(processDays, that.processDays) && Objects.equals(handlaggareDays, that.handlaggareDays) && Objects.equals(effectiveDays, that.effectiveDays)
			&& Objects.equals(deviationFromDate, that.deviationFromDate) && Objects.equals(deviationToDate, that.deviationToDate)
			&& Objects.equals(normInterval, that.normInterval) && Objects.equals(jobbstimulansAmount, that.jobbstimulansAmount) && Objects.equals(note, that.note)
			&& Objects.equals(created, that.created) && Objects.equals(updated, that.updated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, origin, partyId, role, name, processDays, handlaggareDays, effectiveDays, included, deviationFromDate, deviationToDate, normInterval,
			jobbstimulansAmount, deleted, note, created, updated);
	}

	@Override
	public String toString() {
		return "NormPersonRow{" +
			"id='" + id + '\'' +
			", origin='" + origin + '\'' +
			", partyId='" + partyId + '\'' +
			", role='" + role + '\'' +
			", name='" + name + '\'' +
			", processDays=" + processDays +
			", handlaggareDays=" + handlaggareDays +
			", effectiveDays=" + effectiveDays +
			", included=" + included +
			", deviationFromDate=" + deviationFromDate +
			", deviationToDate=" + deviationToDate +
			", normInterval='" + normInterval + '\'' +
			", jobbstimulansAmount=" + jobbstimulansAmount +
			", deleted=" + deleted +
			", note='" + note + '\'' +
			", created=" + created +
			", updated=" + updated +
			'}';
	}
}
